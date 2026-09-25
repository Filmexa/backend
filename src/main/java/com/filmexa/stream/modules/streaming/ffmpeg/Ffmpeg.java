package com.filmexa.stream.modules.streaming.ffmpeg;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.filmexa.stream.modules.streaming.config.StreamProperties;
import com.filmexa.stream.modules.streaming.dto.AudioTrack;
import com.filmexa.stream.modules.streaming.dto.MediaInfo;
import com.filmexa.stream.modules.streaming.dto.SubtitleTrack;
import com.filmexa.stream.modules.streaming.enums.Resolution;
import com.filmexa.stream.modules.streaming.exception.StreamNotReadyException;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The only place that shells out to ffmpeg/ffprobe: probing a file, and encoding one
 * HLS segment straight into the HTTP response.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Ffmpeg {

    private final StreamProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Caps how many ffmpeg processes may burn CPU at once. */
    private Semaphore slots;

    @PostConstruct
    void initialise() {
        slots = new Semaphore(Math.max(1, properties.getMaxConcurrentTranscodes()));
    }

    /**
     * Reads duration and dimensions from the file. Works on a partially downloaded movie
     * as long as the header is present.
     */
    public MediaInfo probe(Path file) {
        List<String> command = List.of(
                properties.getFfprobePath(),
                "-v", "error",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                file.toAbsolutePath().toString());

        Process process = null;
        try {
            process = new ProcessBuilder(command).start();
            // Both pipes must be drained at once: if ffprobe fills stderr while we are
            // reading stdout, it blocks and we time out waiting on a stalled process.
            StringBuilder errorBuffer = new StringBuilder();
            Process running = process;
            Thread errThread = new Thread(
                    () -> errorBuffer.append(drain(running.getErrorStream())), "ffprobe-stderr");
            errThread.start();

            String json = drain(process.getInputStream());
            errThread.join(5000);
            String errors = errorBuffer.toString();

            if (!process.waitFor(properties.getProbeTimeoutSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new StreamNotReadyException("ffprobe timed out for " + file, 10);
            }
            if (process.exitValue() != 0) {
                log.warn("ffprobe failed for {}: {}", file, errors.trim());
                throw new StreamNotReadyException("Not enough of this movie has downloaded yet", 10);
            }

            JsonNode root = objectMapper.readTree(json);
            JsonNode video = videoStream(root);
            double duration = root.path("format").path("duration").asDouble(0);
            if (duration <= 0 && video != null) {
                duration = video.path("duration").asDouble(0);
            }

            if (video == null || duration <= 0) {
                throw new StreamNotReadyException("Movie header is not readable yet", 10);
            }

            MediaInfo info = new MediaInfo(
                    file.toAbsolutePath(),
                    video.path("width").asInt(0),
                    video.path("height").asInt(0),
                    duration,
                    audioTracks(root),
                    subtitleTracks(root));

            log.info("Probed {}: {}x{}, {}s, {} subtitle track(s)", file.getFileName(),
                    info.width(), info.height(), (long) info.durationSeconds(),
                    info.subtitles().size());
            return info;
        } catch (IOException e) {
            throw new StreamNotReadyException("Could not probe this movie: " + e.getMessage(), 10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StreamNotReadyException("Interrupted while probing", 5);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * Encodes exactly one segment and returns it.
     *
     * <p>The bytes are collected in memory rather than streamed straight to the socket,
     * because ffmpeg only reveals a failure once it exits - and by then a streamed
     * response has already committed its 200, leaving no way to answer 503 instead.
     * A segment is a couple of MB and the semaphore caps how many exist at once.
     *
     * <p>Each segment is an independent encode, so it always starts on a keyframe, which
     * is what makes the segments splice together cleanly. {@code -output_ts_offset} puts
     * its timestamps where the playlist says they belong, keeping the player's timeline
     * continuous across segments.
     */
    public byte[] encodeSegment(MediaInfo info, Resolution resolution, int segmentIndex,
                               int audioTrackIndex) {
        double start = (double) segmentIndex * properties.getSegmentSeconds();
        int videoKbps = resolution.getVideoBitrateKbps();

        List<String> command = List.of(
                properties.getFfmpegPath(),
                "-nostdin",
                "-hide_banner",
                "-loglevel", "error",
                // Fast seek: before -i, so ffmpeg jumps via the index instead of decoding
                // everything from the start of the file.
                "-ss", seconds(start),
                "-i", info.file().toString(),
                "-t", String.valueOf(properties.getSegmentSeconds()),
                "-map", "0:v:0",
                // The original-language track, which is not always the first one the
                // file lists - a dub is often muxed ahead of it.
                "-map", "0:a:" + audioTrackIndex + "?",
                "-vf", "scale=-2:" + resolution.getHeight(),
                "-c:v", "libx264",
                "-preset", properties.getPreset(),
                "-profile:v", "high",
                "-level", "4.1",
                "-pix_fmt", "yuv420p",
                "-b:v", videoKbps + "k",
                "-maxrate", (int) (videoKbps * 1.07) + "k",
                "-bufsize", (videoKbps * 2) + "k",
                // Normalising audio to stereo AAC is what makes ac3/dts/truehd playable.
                "-c:a", "aac",
                "-b:a", resolution.getAudioBitrateKbps() + "k",
                "-ac", "2",
                "-f", "mpegts",
                "-output_ts_offset", seconds(start),
                "-muxdelay", "0",
                "-muxpreload", "0",
                "pipe:1");

        return runCollecting(command, segmentIndex);
    }

    private byte[] runCollecting(List<String> command, int segmentIndex) {
        boolean acquired;
        try {
            // Rather than queueing without limit, make a viewer who arrives while every
            // encoder is busy buffer and retry instead of piling up ffmpeg processes.
            acquired = slots.tryAcquire(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StreamNotReadyException("Interrupted waiting for an encoder", 5);
        }
        if (!acquired) {
            throw new StreamNotReadyException("All encoders are busy, try again shortly", 3);
        }

        Process process = null;
        try {
            process = new ProcessBuilder(command).start();
            StringBuilder errors = new StringBuilder();
            Process running = process;
            Thread errThread = new Thread(() -> errors.append(drain(running.getErrorStream())), "ffmpeg-stderr");
            errThread.start();

            java.io.ByteArrayOutputStream collected = new java.io.ByteArrayOutputStream(1 << 20);
            byte[] chunk = new byte[64 * 1024];
            try (InputStream out = process.getInputStream()) {
                int read;
                while ((read = out.read(chunk)) != -1) {
                    collected.write(chunk, 0, read);
                }
            }

            if (!process.waitFor(properties.getTranscodeTimeoutSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("ffmpeg timed out on segment " + segmentIndex);
            }
            errThread.join(5000);

            if (process.exitValue() != 0) {
                log.warn("ffmpeg failed on segment {}: {}", segmentIndex, errors.toString().trim());
                throw new StreamNotReadyException("Segment " + segmentIndex + " is buffering", 2);
            }

            // Asked to seek past the end of the data actually on disk, ffmpeg exits 0 and
            // simply produces nothing. Verified against a truncated file: exit=0, 0 bytes.
            // The availability gate should have caught this, but it works off a
            // bytes-to-seconds estimate that variable bitrate can throw off - so treat an
            // empty segment as "not downloaded yet" and let the player retry, rather than
            // handing it an empty 200 it cannot do anything with.
            if (collected.size() == 0) {
                log.warn("Segment {} encoded to zero bytes - the source data is probably "
                        + "not on disk yet: {}", segmentIndex, errors.toString().trim());
                throw new StreamNotReadyException(
                        "Segment " + segmentIndex + " produced no data yet", 5);
            }

            return collected.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not encode segment " + segmentIndex, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted encoding segment " + segmentIndex, e);
        } finally {
            slots.release();
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /** Every audio stream in the file, in the order ffmpeg's {@code -map 0:a:N} counts them. */
    private List<AudioTrack> audioTracks(JsonNode probe) {
        List<AudioTrack> tracks = new ArrayList<>();
        int index = 0;

        for (JsonNode stream : probe.path("streams")) {
            if (!"audio".equals(stream.path("codec_type").asText())) {
                continue;
            }
            JsonNode tags = stream.path("tags");
            tracks.add(new AudioTrack(
                    index++,
                    tags.path("language").asText("und"),
                    tags.path("title").asText(null)));
        }
        return tracks;
    }

    /**
     * Plenty of rips leave the disposition flags at zero and say it in the track name
     * instead, so the name is checked too. "CC" and "HI" are left out on purpose: two
     * letters match far too much by accident.
     */
    private static final java.util.regex.Pattern FORCED_TITLE =
            java.util.regex.Pattern.compile("\\bforced\\b", java.util.regex.Pattern.CASE_INSENSITIVE);

    private static final java.util.regex.Pattern SDH_TITLE = java.util.regex.Pattern.compile(
            "\\bsdh\\b|hearing[ -]?impaired|closed[ -]?caption", java.util.regex.Pattern.CASE_INSENSITIVE);

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** Subtitle codecs that carry actual text, and so have a WebVTT equivalent. */
    private static final java.util.Set<String> TEXT_SUBTITLE_CODECS =
            java.util.Set.of("subrip", "srt", "ass", "ssa", "mov_text", "webvtt", "text", "microdvd");

    /**
     * Every subtitle stream in the file, in the order ffmpeg's {@code -map 0:s:N} counts them.
     *
     * <p>Bitmap formats (PGS, VobSub, DVB) are listed but flagged unconvertible: they hold
     * images of text, so turning them into WebVTT would need OCR.
     */
    private List<SubtitleTrack> subtitleTracks(JsonNode probe) {
        List<SubtitleTrack> tracks = new ArrayList<>();
        int index = 0;

        for (JsonNode stream : probe.path("streams")) {
            if (!"subtitle".equals(stream.path("codec_type").asText())) {
                continue;
            }
            String codec = stream.path("codec_name").asText("").toLowerCase(Locale.ROOT);
            JsonNode tags = stream.path("tags");
            JsonNode disposition = stream.path("disposition");
            String title = tags.path("title").asText(null);

            tracks.add(new SubtitleTrack(
                    index++,
                    tags.path("language").asText("und"),
                    title,
                    TEXT_SUBTITLE_CODECS.contains(codec),
                    disposition.path("forced").asInt(0) == 1 || FORCED_TITLE.matcher(nullToEmpty(title)).find(),
                    disposition.path("hearing_impaired").asInt(0) == 1
                            || SDH_TITLE.matcher(nullToEmpty(title)).find()));
        }
        return tracks;
    }

    /**
     * Converts one subtitle stream to WebVTT at {@code destination}.
     *
     * <p>Unlike a segment this reads the whole container, so it is slow on a large file -
     * which is exactly why the result is written to disk and kept, rather than piped per
     * request like segments are.
     */
    public void extractSubtitle(MediaInfo info, int trackIndex, Path destination) {
        List<String> command = List.of(
                properties.getFfmpegPath(),
                "-nostdin",
                "-hide_banner",
                "-loglevel", "error",
                "-i", info.file().toString(),
                "-map", "0:s:" + trackIndex,
                "-c:s", "webvtt",
                "-f", "webvtt",
                "-y",
                destination.toAbsolutePath().toString());

        Process process = null;
        try {
            process = new ProcessBuilder(command).start();
            StringBuilder errorBuffer = new StringBuilder();
            Process running = process;
            Thread errThread = new Thread(
                    () -> errorBuffer.append(drain(running.getErrorStream())), "ffmpeg-sub-stderr");
            errThread.start();
            drain(process.getInputStream());

            if (!process.waitFor(properties.getTranscodeTimeoutSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("Subtitle extraction timed out");
            }
            errThread.join(5000);

            if (process.exitValue() != 0) {
                throw new IllegalStateException(
                        "Could not extract subtitle track " + trackIndex + ": " + errorBuffer.toString().trim());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not extract subtitle track " + trackIndex, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted extracting subtitle track " + trackIndex, e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /** The first video stream, or null for a file that has none (or no readable header yet). */
    private JsonNode videoStream(JsonNode probe) {
        for (JsonNode stream : probe.path("streams")) {
            if ("video".equals(stream.path("codec_type").asText())) {
                return stream;
            }
        }
        return null;
    }

    private String drain(InputStream input) {
        try (InputStream in = input) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    /** ffmpeg rejects values like "1.0E-4"; keep seek values plain. */
    private String seconds(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }
}
