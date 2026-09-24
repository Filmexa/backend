package com.filmexa.stream.modules.streaming.serviceImpl;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.filmexa.stream.common.exception.NotFoundException;
import com.filmexa.stream.modules.download.dto.DownloadProgressDto;
import com.filmexa.stream.modules.download.entity.MovieDownload;
import com.filmexa.stream.modules.download.enums.DownloadStatus;
import com.filmexa.stream.modules.download.repo.MovieDownloadRepository;
import com.filmexa.stream.modules.download.dto.DownloadRequestDto;
import com.filmexa.stream.modules.download.service.TorrentDownloadService;
import com.filmexa.stream.modules.streaming.config.StreamProperties;
import com.filmexa.stream.modules.streaming.dto.MediaInfo;
import com.filmexa.stream.modules.streaming.dto.StreamSessionDto;
import com.filmexa.stream.modules.streaming.dto.SubtitleTrack;
import com.filmexa.stream.modules.streaming.dto.SubtitleTrackDto;
import com.filmexa.stream.modules.streaming.dto.VariantDto;
import com.filmexa.stream.modules.streaming.enums.Resolution;
import com.filmexa.stream.modules.streaming.enums.StreamState;
import com.filmexa.stream.modules.streaming.exception.StreamNotReadyException;
import com.filmexa.stream.modules.streaming.ffmpeg.Ffmpeg;
import com.filmexa.stream.modules.streaming.playlist.PlaylistBuilder;
import com.filmexa.stream.modules.streaming.security.StreamTokenService;
import com.filmexa.stream.modules.streaming.service.StreamService;
import com.filmexa.stream.modules.streaming.util.Languages;
import com.filmexa.stream.modules.torrent.service.TorrentService;
import com.filmexa.stream.modules.users.entity.User;
import com.filmexa.stream.modules.users.enums.PreferredLanguage;
import com.filmexa.stream.modules.torrent.dto.TorrentResultDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamServiceImpl implements StreamService {

    private static final Set<String> VIDEO_EXTENSIONS =
            Set.of("mp4", "mkv", "avi", "mov", "m4v", "webm", "wmv", "flv", "mpg", "mpeg", "ts");

    private final Ffmpeg ffmpeg;
    private final PlaylistBuilder playlistBuilder;
    private final StreamTokenService streamTokenService;
    private final StreamProperties properties;
    private final TorrentDownloadService torrentDownloadService;
    private final MovieDownloadRepository movieDownloadRepository;
    private final TorrentService torrentService;

    @Value("${app.video-storage-path:./data/movies}")
    private String videoStoragePath;

    private final Map<Long, MediaInfo> probes = new ConcurrentHashMap<>();

    @Override
    public StreamSessionDto createSession(Long movieId, String imdbId, User viewer) {
        MovieDownload download = ensureDownloadStarted(movieId, imdbId);

        MediaInfo info;
        try {
            info = mediaInfo(movieId);
            // READY has to mean "the player will actually get its first segment", not just
            // "the download flipped a flag" - so check the real gate the segments use.
            requireDownloaded(movieId, info, 0);
        } catch (StreamNotReadyException | NotFoundException notReadyYet) {
            log.debug("Movie {} not playable yet: {}", movieId, notReadyYet.getMessage());
            return preparing(movieId, download);
        }

        markWatched(movieId);
        String token = streamTokenService.issue(viewer.getUsername());

        return StreamSessionDto.builder()
                .movieId(movieId)
                .state(StreamState.READY)
                .downloadStatus(currentStatus(movieId, download))
                .downloadProgressPercentage(currentProgress(movieId, download))
                .manifestUrl(url(movieId, "master.m3u8", token))
                .token(token)
                .expiresInSeconds(streamTokenService.getTtlSeconds())
                .durationSeconds(info.durationSeconds())
                .variants(variantsFor(movieId, info, token))
                .subtitles(subtitlesFor(movieId, info, token, viewer))
                .build();
    }

    /**
     * Makes sure a download exists and is running for this movie, resolving a magnet the
     * first time. The frontend never supplies the magnet - it only knows the movie id.
     */
    private MovieDownload ensureDownloadStarted(Long movieId, String  imdbId) {
        Optional<MovieDownload> existing = movieDownloadRepository.findByMovieId(movieId);

        if (existing.isPresent()) {
            DownloadStatus status = existing.get().getStatus();

            // Already on disk - nothing to do.
            if (status == DownloadStatus.COMPLETED) {
                return existing.get();
            }

            // Anything else needs a live torrent client. A row can say DOWNLOADING while
            // no client exists at all - the worker keeps that state in memory, so a
            // restart loses it and the stored status is left stale. Without this check
            // such a movie polls PREPARING forever and never resumes.
            if (torrentDownloadService.isActive(movieId)) {
                return existing.get();
            }

            log.info("Movie {} is {} but no torrent is running, restarting the download",
                    movieId, status);
        }

        Optional<TorrentResultDto> torrent = torrentService.resolve(imdbId);

        if (torrent.isPresent()) {
            // Mohssin part (Download)
            DownloadRequestDto request = new DownloadRequestDto();
            request.setMovieId(movieId);
            System.out.println("Magnet URL: " + torrent.get().getMagnet());
            request.setMagnetUrl(torrent.get().getMagnet());
            return torrentDownloadService.startDownload(request);
        }
        throw new RuntimeException("Failed to resolve torrent for movie: " + imdbId);
    }

    private StreamSessionDto preparing(Long movieId, MovieDownload download) {
        return StreamSessionDto.builder()
                .movieId(movieId)
                .state(StreamState.PREPARING)
                .downloadStatus(currentStatus(movieId, download))
                .downloadProgressPercentage(currentProgress(movieId, download))
                .build();
    }

    private DownloadStatus currentStatus(Long movieId, MovieDownload download) {
        DownloadProgressDto progress = torrentDownloadService.getProgress(movieId);
        if (progress != null) {
            return progress.getStatus();
        }
        return download == null ? DownloadStatus.PENDING : download.getStatus();
    }

    private double currentProgress(Long movieId, MovieDownload download) {
        DownloadProgressDto progress = torrentDownloadService.getProgress(movieId);
        if (progress != null) {
            return progress.getProgressPercentage();
        }
        if (download == null || download.getTotalBytes() <= 0) {
            return 0;
        }
        return ((double) download.getDownloadedBytes() / download.getTotalBytes()) * 100.0;
    }

    @Override
    public String masterPlaylist(Long movieId, String token) {
        MediaInfo info = mediaInfo(movieId);
        markWatched(movieId);
        return playlistBuilder.master(info, Resolution.ladderFor(info.height()), token);
    }

    @Override
    public String mediaPlaylist(Long movieId, int height, String token) {
        MediaInfo info = mediaInfo(movieId);
        return playlistBuilder.media(info, resolveRung(info, height), token);
    }

    @Override
    public Segment prepareSegment(Long movieId, int height, int segmentIndex) {
        MediaInfo info = mediaInfo(movieId);
        Resolution resolution = resolveRung(info, height);

        if (segmentIndex < 0 || segmentIndex >= playlistBuilder.segmentCount(info)) {
            throw new NotFoundException("Segment " + segmentIndex + " is outside movie " + movieId);
        }

        requireDownloaded(movieId, info, segmentIndex);
        return new Segment(info, resolution, segmentIndex);
    }

    @Override
    public byte[] segmentBytes(Segment segment) {
        return ffmpeg.encodeSegment(segment.info(), segment.resolution(), segment.index());
    }

    @Override
    public Path subtitle(Long movieId, int trackIndex) {
        MediaInfo info = mediaInfo(movieId);

        SubtitleTrack track = info.subtitles().stream()
                .filter(candidate -> candidate.index() == trackIndex)
                .filter(SubtitleTrack::convertible)
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "No convertible subtitle track " + trackIndex + " for movie " + movieId));

        Path destination = subtitleDirectory(movieId).resolve(trackIndex + ".vtt");
        if (Files.isRegularFile(destination)) {
            return destination;
        }

        // Extracting scans the entire container, so unlike a segment this is worth keeping
        // once produced. Write to a temp file and move it into place, so a half-extracted
        // file is never served by the check above.
        Path temporary = null;
        try {
            Files.createDirectories(destination.getParent());
            temporary = Files.createTempFile(destination.getParent(), "sub-", ".vtt.part");

            ffmpeg.extractSubtitle(info, trackIndex, temporary);

            Files.move(temporary, destination,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            temporary = null;

            log.info("Extracted subtitle track {} for movie {}", trackIndex, movieId);
            return destination;
        } catch (IOException e) {
            throw new IllegalStateException("Could not write subtitle for movie " + movieId, e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException e) {
                    log.debug("Could not remove temp subtitle {}: {}", temporary, e.getMessage());
                }
            }
        }
    }

    private Path subtitleDirectory(Long movieId) {
        return Paths.get(videoStoragePath, movieId.toString(), "subtitles").toAbsolutePath();
    }

    /**
     * Builds the subtitle menu for one viewer.
     *
     * <p>Every convertible track is offered, so anything in the file stays selectable. On
     * top of that the subject asks for a track to be *active* when the viewer would not
     * understand the audio: if the movie's audio language differs from their preferred
     * language, the matching subtitle is marked as the default, falling back to English.
     * When the audio is already in their language nothing is auto-enabled.
     *
     * <p>The list is ordered preferred-language first, then English, so the browser's own
     * subtitle menu leads with the useful entries.
     */
    private List<SubtitleTrackDto> subtitlesFor(Long movieId, MediaInfo info, String token, User viewer) {
        String preferred = preferredLanguageOf(viewer);
        String audio = Languages.toBcp47(info.audioLanguage());
        boolean viewerUnderstandsAudio = preferred.equals(audio);

        List<SubtitleTrack> usable = new ArrayList<>(info.subtitles().stream()
                .filter(SubtitleTrack::convertible)
                .toList());

        usable.sort(Comparator
                .comparing((SubtitleTrack track) -> !matches(track, preferred))
                .thenComparing(track -> !matches(track, "en"))
                .thenComparingInt(SubtitleTrack::index));

        SubtitleTrack autoEnable = viewerUnderstandsAudio ? null : pickDefault(usable, preferred);

        return usable.stream()
                .map(track -> SubtitleTrackDto.builder()
                        .language(Languages.toBcp47(track.language()))
                        .label(labelFor(track))
                        .url(url(movieId, "subtitles/" + track.index() + ".vtt", token))
                        .defaultTrack(track.equals(autoEnable))
                        .build())
                .toList();
    }

    /** The viewer's language, or English when they have not set one. */
    private String preferredLanguageOf(User viewer) {
        PreferredLanguage preference = viewer == null ? null : viewer.getPreferredLanguage();
        return preference == null ? "en" : preference.getDisplayName();
    }

    /** Their language if the file has it, otherwise English, otherwise nothing. */
    private SubtitleTrack pickDefault(List<SubtitleTrack> usable, String preferred) {
        return usable.stream()
                .filter(track -> matches(track, preferred))
                .findFirst()
                .or(() -> usable.stream().filter(track -> matches(track, "en")).findFirst())
                .orElse(null);
    }

    private boolean matches(SubtitleTrack track, String bcp47) {
        return Languages.toBcp47(track.language()).equals(bcp47);
    }

    private String labelFor(SubtitleTrack track) {
        if (track.title() != null && !track.title().isBlank()) {
            return track.title();
        }
        return Languages.displayName(track.language());
    }

    /** Probes on first use, then serves from memory. */
    private MediaInfo mediaInfo(Long movieId) {
        MediaInfo cached = probes.get(movieId);
        // The cleanup job can delete a movie out from under a cached probe, so confirm
        // the file is still there rather than handing back a path to nothing.
        if (cached != null && Files.isRegularFile(cached.file())) {
            return cached;
        }
        probes.remove(movieId);

        Path file = locate(movieId)
                .orElseThrow(() -> new NotFoundException(
                        "No downloaded video file found for movie " + movieId));

        MediaInfo info = ffmpeg.probe(file);
        probes.put(movieId, info);
        return info;
    }

    /**
     * Refuses a segment whose bytes are not on disk yet.
     *
     * <p>Pieces are downloaded strictly in order, so the fraction of the file we hold maps
     * directly onto a position in the timeline. A segment is playable once the download
     * head has passed its end, plus a readahead margin so playback does not stall again
     * on the very next segment.
     */
    private void requireDownloaded(Long movieId, MediaInfo info, int segmentIndex) {
        double segmentEnd = (double) (segmentIndex + 1) * properties.getSegmentSeconds();
        double required = Math.min(segmentEnd + properties.getReadaheadSeconds(), info.durationSeconds());
        double playable = playableSeconds(movieId, info);

        if (playable + 0.001 < required) {
            log.debug("Segment {} of movie {} needs {}s downloaded, have {}s",
                    segmentIndex, movieId, (long) required, (long) playable);
            throw new StreamNotReadyException(
                    "Segment " + segmentIndex + " has not been downloaded yet", 5);
        }
    }

    /** How many seconds from the start of the movie are estimated to be on disk. */
    private double playableSeconds(Long movieId, MediaInfo info) {
        DownloadProgressDto progress = torrentDownloadService.getProgress(movieId);

        // No download record at all means the file is simply sitting on disk.
        if (progress == null || progress.getStatus() == DownloadStatus.COMPLETED) {
            return info.durationSeconds();
        }
        if (progress.getTotalBytes() <= 0) {
            return 0;
        }

        double ratio = (double) progress.getDownloadedBytes() / progress.getTotalBytes();
        return Math.min(1.0, ratio) * info.durationSeconds();
    }

    /**
     * Finds the movie file. The download worker records its relative path once the torrent
     * metadata resolves; otherwise we fall back to the largest video file in the folder.
     */
    private Optional<Path> locate(Long movieId) {
        Path directory = Paths.get(videoStoragePath, movieId.toString()).toAbsolutePath();

        Optional<Path> recorded = movieDownloadRepository.findByMovieId(movieId)
                .map(MovieDownload::getFileName)
                .filter(name -> name != null && !name.isBlank())
                .map(directory::resolve)
                .filter(Files::isRegularFile);

        if (recorded.isPresent()) {
            return recorded;
        }

        if (!Files.isDirectory(directory)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.walk(directory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(this::isVideoFile)
                    .max(Comparator.comparingLong(this::sizeOf));
        } catch (IOException e) {
            log.warn("Could not scan {} for a video file: {}", directory, e.getMessage());
            return Optional.empty();
        }
    }

    private boolean isVideoFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return dot >= 0 && VIDEO_EXTENSIONS.contains(name.substring(dot + 1));
    }

    private long sizeOf(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0L;
        }
    }

    private Resolution resolveRung(MediaInfo info, int height) {
        List<Resolution> ladder = Resolution.ladderFor(info.height());
        return Resolution.byHeight(height)
                .filter(ladder::contains)
                .orElseThrow(() -> new NotFoundException(height + "p is not available for this movie"));
    }

    private List<VariantDto> variantsFor(Long movieId, MediaInfo info, String token) {
        return Resolution.ladderFor(info.height()).stream()
                .map(resolution -> VariantDto.builder()
                        .label(resolution.getLabel())
                        .height(resolution.getHeight())
                        .width(playlistBuilder.scaledWidth(info, resolution))
                        .bandwidth(resolution.getTotalBitrateBps())
                        .url(url(movieId, resolution.getHeight() + "/index.m3u8", token))
                        .build())
                .toList();
    }

    private String url(Long movieId, String suffix, String token) {
        String base = "/api/stream/" + movieId + "/" + suffix;
        if (token == null || token.isBlank()) {
            return base;
        }
        return base + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    /**
     * Keeps the 30-day cleanup rule honest: a movie someone is watching right now must
     * not look stale to the cleanup job.
     */
    private void markWatched(Long movieId) {
        movieDownloadRepository.findByMovieId(movieId).ifPresent(download -> {
            download.setLastWatchedAt(LocalDateTime.now());
            movieDownloadRepository.save(download);
        });
    }
}
