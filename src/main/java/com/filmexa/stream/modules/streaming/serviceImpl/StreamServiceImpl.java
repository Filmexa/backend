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
import java.util.stream.Collectors;
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
import com.filmexa.stream.modules.streaming.dto.AudioTrack;
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
import com.filmexa.stream.modules.moviesExternal.client.MovieProvider;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieProvederData;
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

    /**
     * How much of a torrent's total has to be on disk before a COMPLETED download is
     * taken at its word. The video file is always a little smaller than the torrent.
     */
    private static final double COMPLETE_FILE_RATIO = 0.95;

    private static final Set<String> VIDEO_EXTENSIONS =
            Set.of("mp4", "mkv", "avi", "mov", "m4v", "webm", "wmv", "flv", "mpg", "mpeg", "ts");

    /**
     * Subtitles are offered in English, French and Arabic only. Deriving the set from
     * PreferredLanguage rather than listing the codes again keeps the menu in step with
     * the languages a viewer can actually choose, so the two cannot drift apart.
     */
    private static final Set<String> OFFERED_SUBTITLE_LANGUAGES =
            Stream.of(PreferredLanguage.values())
                    .map(PreferredLanguage::getDisplayName)
                    .collect(Collectors.toUnmodifiableSet());

    private final Ffmpeg ffmpeg;
    private final PlaylistBuilder playlistBuilder;
    private final StreamTokenService streamTokenService;
    private final StreamProperties properties;
    private final TorrentDownloadService torrentDownloadService;
    private final MovieDownloadRepository movieDownloadRepository;
    private final TorrentService torrentService;
    private final MovieProvider movieProvider;

    @Value("${app.video-storage-path:./data/movies}")
    private String videoStoragePath;

    private final Map<Long, MediaInfo> probes = new ConcurrentHashMap<>();

    /** TMDB's original_language per movie. One lookup per movie, then served from memory. */
    private final Map<Long, String> originalLanguages = new ConcurrentHashMap<>();

    @Override
    public StreamSessionDto createSession(Long movieId, String imdbId, User viewer) {
        MovieDownload download = ensureDownloadStarted(movieId, imdbId);

        DownloadProgressDto progress = torrentDownloadService.getProgress(movieId);
        if (progress != null && !progress.isReadyToStream()) {
            return preparing(movieId, download);
        }

        if (torrentDownloadService.isActive(movieId)) {
            Optional<Boolean> headerOnDisk = torrentDownloadService.isRangeDownloaded(movieId, 0.0, 0.001);
            if (headerOnDisk.isPresent() && !headerOnDisk.get()) {
                return preparing(movieId, download);
            }
        }

        MediaInfo info;
        try {
            info = mediaInfo(movieId);
            // READY has to mean "the player will actually get its first segment", not just
            // "the download flipped a flag" - so check the real gate the segments use.
            requireDownloaded(movieId, info, 0);
        } catch (StreamNotReadyException | NotFoundException notReadyYet) {
            log.debug("Movie {} not playable yet: {}", movieId, notReadyYet.getMessage());
            return preparing(movieId, download);
        } catch (Exception e) {
            log.warn("Probe or gate check exception for movie {}: {}", movieId, e.getMessage());
            return preparing(movieId, download);
        }

        markWatched(movieId);
        String token = streamTokenService.issue(viewer.getUsername());

        DownloadProgressDto progressDto = torrentDownloadService.getProgress(movieId);
        Long downloaded = progressDto != null ? progressDto.getDownloadedBytes() : (download != null ? download.getDownloadedBytes() : 0L);
        Long total = progressDto != null ? progressDto.getTotalBytes() : (download != null ? download.getTotalBytes() : 0L);
        Double speed = progressDto != null ? progressDto.getDownloadSpeedBps() : 0.0;

        return StreamSessionDto.builder()
                .movieId(movieId)
                .state(StreamState.READY)
                .downloadStatus(currentStatus(movieId, download))
                .downloadProgressPercentage(currentProgress(movieId, download))
                .downloadedBytes(downloaded)
                .totalBytes(total)
                .downloadSpeedBps(speed)
                .manifestUrl(url(movieId, "master.m3u8", token))
                .token(token)
                .expiresInSeconds(streamTokenService.getTtlSeconds())
                .durationSeconds(info.durationSeconds())
                .playableSeconds(playableSeconds(movieId, info))
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

            // Already on disk - nothing to do, as long as it really is all there. A
            // COMPLETED row over a part-downloaded file is a dead end otherwise: this
            // early return is what stops the download ever being picked up again.
            if (status == DownloadStatus.COMPLETED && isFileComplete(existing.get())) {
                return existing.get();
            }

            if (status == DownloadStatus.COMPLETED) {
                log.warn("Movie {} is marked COMPLETED but the file on disk is short,"
                        + " downloading the rest", movieId);
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
            return existing.isPresent()
                    ? torrentDownloadService.restartDownload(request)
                    : torrentDownloadService.startDownload(request);
        }
        // No provider carries this film - that is a 404 for the caller, not a server fault.
        throw new NotFoundException("No torrent available for movie " + movieId);
    }

    private StreamSessionDto preparing(Long movieId, MovieDownload download) {
        DownloadProgressDto progress = torrentDownloadService.getProgress(movieId);
        Long downloaded = progress != null ? progress.getDownloadedBytes() : (download != null ? download.getDownloadedBytes() : 0L);
        Long total = progress != null ? progress.getTotalBytes() : (download != null ? download.getTotalBytes() : 0L);
        Double speed = progress != null ? progress.getDownloadSpeedBps() : 0.0;

        return StreamSessionDto.builder()
                .movieId(movieId)
                .state(StreamState.PREPARING)
                .downloadStatus(currentStatus(movieId, download))
                .downloadProgressPercentage(currentProgress(movieId, download))
                .downloadedBytes(downloaded)
                .totalBytes(total)
                .downloadSpeedBps(speed)
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
        resolveRung(info, height);
        return playlistBuilder.media(info, token);
    }

    @Override
    public Segment prepareSegment(Long movieId, int height, int segmentIndex) {
        MediaInfo info = mediaInfo(movieId);
        Resolution resolution = resolveRung(info, height);

        if (segmentIndex < 0 || segmentIndex >= playlistBuilder.segmentCount(info)) {
            throw new NotFoundException("Segment " + segmentIndex + " is outside movie " + movieId);
        }

        requireDownloaded(movieId, info, segmentIndex);
        return new Segment(info, resolution, segmentIndex, originalAudioIndex(movieId, info));
    }

    @Override
    public byte[] segmentBytes(Segment segment) {
        return ffmpeg.encodeSegment(segment.info(), segment.resolution(), segment.index(),
                segment.audioTrackIndex());
    }

    @Override
    public Path subtitle(Long movieId, int trackIndex) {
        MediaInfo info = mediaInfo(movieId);

        SubtitleTrack track = info.subtitles().stream()
                .filter(candidate -> candidate.index() == trackIndex)
                .filter(SubtitleTrack::convertible)
                .filter(this::offered)
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
     * <p>Every convertible track in an offered language is listed, so anything the viewer
     * could read stays selectable. On top of that the subject asks for a track to be
     * *active* when the viewer would not understand the audio: if the movie's audio
     * language differs from their preferred language, the matching subtitle is marked as
     * the default, falling back to English.
     * When the audio is already in their language nothing is auto-enabled.
     *
     * <p>The list is ordered preferred-language first, then English, so the browser's own
     * subtitle menu leads with the useful entries.
     */
    private List<SubtitleTrackDto> subtitlesFor(Long movieId, MediaInfo info, String token, User viewer) {
        String preferred = preferredLanguageOf(viewer);
        String audio = Languages.toBcp47(playedAudioTrack(movieId, info).language());
        boolean viewerUnderstandsAudio = preferred.equals(audio);

        List<SubtitleTrack> usable = new ArrayList<>(info.subtitles().stream()
                .filter(SubtitleTrack::convertible)
                .filter(this::offered)
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

    /**
     * The audio stream to play: the one in the film's original language.
     *
     * <p>A release commonly muxes a dub ahead of the original, so the first stream is not a
     * safe default. TMDB knows what the film was shot in, which is the only reliable
     * signal - the file's own tags say what each track is, never which is the original.
     *
     * <p>Falls back to the first track when TMDB has no answer or the file carries nothing
     * in that language, which is also the right answer for a single-audio release.
     */
    private AudioTrack playedAudioTrack(Long movieId, MediaInfo info) {
        if (info.audioTracks().isEmpty()) {
            return new AudioTrack(0, "und", null);
        }
        AudioTrack first = info.audioTracks().get(0);

        String original = originalLanguageOf(movieId);
        if (original == null || original.isBlank()) {
            return first;
        }

        return info.audioTracks().stream()
                .filter(track -> Languages.toBcp47(track.language()).equals(original))
                .findFirst()
                .orElse(first);
    }

    private int originalAudioIndex(Long movieId, MediaInfo info) {
        return playedAudioTrack(movieId, info).index();
    }

    /**
     * TMDB's original_language for this movie, or null when it cannot be reached. A failure
     * here must not stop playback, so it degrades to "use the first audio track".
     */
    private String originalLanguageOf(Long movieId) {
        String cached = originalLanguages.get(movieId);
        if (cached != null) {
            return cached;
        }

        String language;
        try {
            MovieProvederData movie = movieProvider.getMovieById("en-US", movieId.intValue());
            language = movie == null ? null : movie.getOriginal_language();
        } catch (RuntimeException e) {
            log.warn("Could not read original language for movie {}: {}", movieId, e.getMessage());
            return null;
        }

        if (language == null || language.isBlank()) {
            return null;
        }
        // Only a real answer is cached: caching the failure would pin this movie to the
        // wrong audio track until the next restart, for what may be a passing outage.
        String normalised = language.trim().toLowerCase(Locale.ROOT);
        originalLanguages.put(movieId, normalised);
        return normalised;
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

    /**
     * Whether a track belongs in the menu at all.
     *
     * <p>Excluded: languages we do not offer, untagged tracks, forced tracks - which only
     * cover signs and foreign dialogue, so they look like a broken full subtitle - and SDH,
     * whose sound-effect and speaker annotations are noise to a viewer who can hear.
     */
    private boolean offered(SubtitleTrack track) {
        return OFFERED_SUBTITLE_LANGUAGES.contains(Languages.toBcp47(track.language()))
                && !track.forced()
                && !track.hearingImpaired();
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
        double segmentStart = (double) segmentIndex * properties.getSegmentSeconds();

        // Ask the torrent which pieces it actually holds. After a seek the data is no
        // longer one run from the start, so a "bytes downloaded" estimate would keep
        // refusing the very segments the seek just fetched.
        if (info.durationSeconds() > 0) {
            Optional<Boolean> downloaded = torrentDownloadService.isRangeDownloaded(movieId,
                    segmentStart / info.durationSeconds(), required / info.durationSeconds());

            if (downloaded.isPresent()) {
                if (downloaded.get()) {
                    return;
                }
                requestSeek(movieId, info, segmentStart);
                log.debug("Segment {} of movie {} is not on disk yet", segmentIndex, movieId);
                throw new StreamNotReadyException(
                        "Segment " + segmentIndex + " has not been downloaded yet", 5);
            }
        }

        // Nothing running to ask - fall back to how much of the file is on disk, which is
        // a prefix as long as no seek has happened.
        double playable = playableSeconds(movieId, info);

        if (playable + 0.001 < required) {
            requestSeek(movieId, info, segmentStart);
            log.debug("Segment {} of movie {} needs {}s downloaded, have {}s",
                    segmentIndex, movieId, (long) required, (long) playable);
            throw new StreamNotReadyException(
                    "Segment " + segmentIndex + " has not been downloaded yet", 5);
        }
    }

    /** How many seconds from the start of the movie are estimated to be on disk. */
    /**
     * Moves the download to what the viewer is trying to watch, rather than making them
     * wait for everything in between - ten minutes of film is a few hundred MB, not the
     * remaining hour of the torrent. Time maps onto bytes only roughly for variable
     * bitrate video, so aim a segment early and let the sequential order carry on.
     */
    private void requestSeek(Long movieId, MediaInfo info, double segmentStart) {
        if (info.durationSeconds() <= 0) {
            return;
        }
        double target = Math.max(0, segmentStart - properties.getSegmentSeconds());
        torrentDownloadService.seek(movieId, target / info.durationSeconds());
    }

    private double playableSeconds(Long movieId, MediaInfo info) {
        DownloadProgressDto progress = torrentDownloadService.getProgress(movieId);

        // No download record at all means the file is simply sitting on disk.
        if (progress == null) {
            return info.durationSeconds();
        }

        long totalBytes = progress.getTotalBytes();

        // The size of the file is the one number that cannot go stale: pieces arrive in
        // order, so the file grows as they land. A status of COMPLETED on a row whose
        // download really stopped a third of the way through is what let the player ask
        // for a segment two hours past the last byte on disk and 503 forever.
        long available = Math.max(progress.getDownloadedBytes(), sizeOf(info.file()));

        if (totalBytes <= 0) {
            return progress.getStatus() == DownloadStatus.COMPLETED ? info.durationSeconds() : 0;
        }

        if (progress.getStatus() == DownloadStatus.COMPLETED) {
            // The torrent's total covers every file in it - samples, nfo - so the video
            // alone lands a little short of it even when the download really did finish.
            if (available >= totalBytes * COMPLETE_FILE_RATIO) {
                return info.durationSeconds();
            }
            log.warn("Movie {} is marked COMPLETED but only {} of {} bytes are on disk;"
                    + " serving what is actually there", movieId, available, totalBytes);
        }

        double ratio = (double) available / totalBytes;
        return Math.min(1.0, ratio) * info.durationSeconds();
    }

    /**
     * Finds the movie file. The download worker records its relative path once the torrent
     * metadata resolves; otherwise we fall back to the largest video file in the folder.
     */
    /** Whether the video file on disk accounts for the torrent this row recorded. */
    private boolean isFileComplete(MovieDownload download) {
        long totalBytes = download.getTotalBytes();
        if (totalBytes <= 0) {
            // Nothing to compare against - rows written before the counters were
            // persisted have no total, so the file has to be taken as it is.
            return true;
        }
        return locate(download.getMovieId())
                .map(this::sizeOf)
                .orElse(0L) >= totalBytes * COMPLETE_FILE_RATIO;
    }

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
