package com.filmexa.stream.modules.download.worker;

import org.springframework.stereotype.Component;
import com.filmexa.stream.modules.download.dto.DownloadProgressDto;
import com.filmexa.stream.modules.download.repo.MovieDownloadRepository;
import com.filmexa.stream.modules.download.config.TorrentRuntimePool;
import com.filmexa.stream.modules.download.selector.SequentialPieceSelector;
import bt.Bt;
import bt.data.Storage;
import bt.data.file.FileSystemStorage;
import bt.data.Bitfield;
import bt.magnet.MagnetUriParser;
import bt.metainfo.TorrentId;
import bt.metainfo.TorrentFile;
import bt.runtime.BtRuntime;
import bt.torrent.TorrentDescriptor;
import bt.torrent.TorrentRegistry;
import bt.metainfo.Torrent;
import bt.peer.IPeerRegistry;
import bt.runtime.BtClient;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.BitSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.filmexa.stream.modules.download.enums.DownloadStatus;
import java.time.LocalDateTime;

@Component
@Slf4j
public class TorrentDownloadWorker {

    /** MP4 sample tables at the end can span multiple pieces; fetch a bounded tail first. */
    private static final long MP4_INDEX_PREFETCH_BYTES = 32L * 1024 * 1024;
    /** ffprobe can try once the header and final MP4 piece are present. */
    private static final int MP4_PROBE_TAIL_PIECES = 2;
    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            "mp4", "mkv", "avi", "mov", "m4v", "webm", "wmv", "flv", "mpg", "mpeg", "ts");

    /** How often we ask the DHT for peers again while a download is getting nowhere. */
    private static final long PEER_TRIGGER_INTERVAL_MS = 5_000;

    /** How often the byte counters are written to the database while downloading. */
    private static final long PROGRESS_PERSIST_INTERVAL_MS = 10_000;

    private final MovieDownloadRepository movieDownloadRepository;
    @Value("${app.video-storage-path:./data/movies}")
    private String videoStoragePath; 
    private final Map<Long, BtClient> activeClients = new ConcurrentHashMap<>(); 
    private final Map<Long, DownloadProgressDto> progressCache = new ConcurrentHashMap<>();

    /**
     * Movies already submitted to the executor, claimed before the task is handed over.
     * A client only lands in {@link #activeClients} once its task actually runs, so
     * without this a caller that polls (the stream session does) queues the same movie
     * again and again, and those duplicates take the slots other movies are waiting for.
     */
    private final Set<Long> submitted = ConcurrentHashMap.newKeySet();

    /** Each running download's selector, so playback can move its playhead. */
    private final Map<Long, SequentialPieceSelector> selectors = new ConcurrentHashMap<>();

    /** Per-download handles for answering "is this part of the film on disk?". */
    private final Map<Long, BtRuntime> runtimes = new ConcurrentHashMap<>();
    private final Map<Long, TorrentId> torrentIds = new ConcurrentHashMap<>();
    /** The selected video file's byte and piece range inside its torrent. */
    private final Map<Long, VideoFileLayout> videoFileLayouts = new ConcurrentHashMap<>();

    private final TorrentRuntimePool runtimePool;
    private final Executor torrentExecutor;

    public TorrentDownloadWorker(MovieDownloadRepository movieDownloadRepository,
            TorrentRuntimePool runtimePool,
            @Qualifier("torrentTaskExecutor") Executor torrentExecutor) {
        this.movieDownloadRepository = movieDownloadRepository;
        this.runtimePool = runtimePool;
        this.torrentExecutor = torrentExecutor;
    }

    public DownloadProgressDto getProgress(Long movieId) {
        return progressCache.get(movieId);
    }

    /** True while the movie is queued for a thread or downloading on one. */
    public boolean isActive(Long movieId) {
        return submitted.contains(movieId) || activeClients.containsKey(movieId);
    }

    /**
     * Moves a running download to the part of the film being watched, so seeking forward
     * does not wait for everything in between.
     *
     * @param fraction how far into the file, 0.0 to 1.0
     * @return true if a download was there to redirect
     */
    public boolean seek(Long movieId, double fraction) {
        SequentialPieceSelector selector = selectors.get(movieId);
        if (selector == null) {
            return false;
        }

        int before = selector.getPlayheadPiece();
        int after = selector.seekToFraction(fraction);
        if (before != after) {
            log.info("Movie {}: viewer seeked to {}% - downloading from piece {} now",
                    movieId, String.format("%.1f", fraction * 100), after);
        }
        return true;
    }

    /**
     * Whether every piece covering a stretch of the file is already on disk.
     *
     * <p>Once a seek has moved the playhead the downloaded data is no longer one run from
     * the start, so "bytes downloaded" says nothing about whether a particular minute of
     * the film can be played. The torrent's own piece bitfield does.
     *
     * @return the answer, or empty when no running download can give one
     */
    public Optional<Boolean> isRangeDownloaded(Long movieId, double fromFraction, double toFraction) {
        BtRuntime runtime = runtimes.get(movieId);
        TorrentId torrentId = torrentIds.get(movieId);
        if (runtime == null || torrentId == null) {
            return Optional.empty();
        }

        try {
            Optional<TorrentDescriptor> descriptor =
                    runtime.service(TorrentRegistry.class).getDescriptor(torrentId);
            if (descriptor.isEmpty() || descriptor.get().getDataDescriptor() == null) {
                // Still fetching the metadata: there are no pieces to ask about yet.
                return Optional.empty();
            }

            Bitfield bitfield = descriptor.get().getDataDescriptor().getBitfield();
            VideoFileLayout layout = videoFileLayouts.get(movieId);
            if (layout == null || layout.pieceSize() <= 0 || layout.fileSize() <= 0) {
                // A magnet's metadata may have arrived between the worker heartbeat and
                // this stream poll. Configure the layout once before probing the file.
                SequentialPieceSelector selector = selectors.get(movieId);
                if (selector != null && configureVideoFileLayout(runtime, torrentId, selector, movieId)) {
                    layout = videoFileLayouts.get(movieId);
                }
            }
            if (layout == null || layout.pieceSize() <= 0 || layout.fileSize() <= 0) {
                return Optional.empty();
            }

            double from = clampFraction(fromFraction);
            double to = Math.max(from, clampFraction(toFraction));
            long startInFile = Math.min(layout.fileSize() - 1,
                    (long) Math.floor(from * layout.fileSize()));
            long endInFile = Math.max(startInFile + 1,
                    Math.min(layout.fileSize(), (long) Math.ceil(to * layout.fileSize())));
            int first = Math.toIntExact((layout.fileOffset() + startInFile) / layout.pieceSize());
            int last = Math.toIntExact((layout.fileOffset() + endInFile - 1) / layout.pieceSize());
            for (int piece = first; piece <= last; piece++) {
                // The range must belong to the selected video. This also protects us from
                // bad metadata or a stale layout after a torrent is replaced.
                if (!layout.videoPieces().get(piece)) {
                    return Optional.of(false);
                }
                if (!bitfield.isComplete(piece)) {
                    return Optional.of(false);
                }
            }
            return Optional.of(true);
        } catch (Exception e) {
            log.debug("Could not read the piece bitfield for movie {}: {}", movieId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * ffprobe needs the file header and, for non-faststart MP4s, the trailing moov index.
     * Waiting for both avoids repeatedly probing a file whose required metadata pieces
     * have not arrived yet.
     */
    Optional<Boolean> isInitialProbeDataDownloaded(Long movieId) {
        BtRuntime runtime = runtimes.get(movieId);
        TorrentId torrentId = torrentIds.get(movieId);
        VideoFileLayout layout = videoFileLayouts.get(movieId);
        if (runtime == null || torrentId == null || layout == null) {
            return Optional.empty();
        }

        try {
            Optional<TorrentDescriptor> descriptor =
                    runtime.service(TorrentRegistry.class).getDescriptor(torrentId);
            if (descriptor.isEmpty() || descriptor.get().getDataDescriptor() == null) {
                return Optional.empty();
            }

            Bitfield bitfield = descriptor.get().getDataDescriptor().getBitfield();
            for (int piece = layout.requiredProbePieces().nextSetBit(0);
                    piece >= 0;
                    piece = layout.requiredProbePieces().nextSetBit(piece + 1)) {
                if (piece >= bitfield.getPiecesTotal() || !bitfield.isComplete(piece)) {
                    return Optional.of(false);
                }
            }
            return Optional.of(true);
        } catch (Exception e) {
            log.debug("Could not read probe pieces for movie {}: {}", movieId, e.getMessage());
            return Optional.empty();
        }
    }

    private double clampFraction(double fraction) {
        return Math.min(1.0, Math.max(0.0, fraction));
    }

    public void stopDownload(Long movieId) {
        BtClient client = activeClients.remove(movieId);
        if (client != null) {   
            client.stop();
            progressCache.remove(movieId);
            selectors.remove(movieId);
            runtimes.remove(movieId);
            torrentIds.remove(movieId);
            videoFileLayouts.remove(movieId);
            log.info("Stopped download for movie with ID: {}", movieId);
            movieDownloadRepository.findByMovieId(movieId).ifPresent(download -> {
                download.setStatus(DownloadStatus.PAUSED);
                movieDownloadRepository.save(download);
            });
        }
    }
    /**
     * Hands the download to the torrent pool. Submitting explicitly rather than through
     * {@code @Async} is what lets the movie be claimed *before* the task is queued, so a
     * second call for the same movie is a no-op instead of a second thread-hogging task.
     *
     * @return false when this movie is already queued or downloading
     */
    public boolean startDownloadAsync(Long movieId, String magnetUrl) {
        if (!submitted.add(movieId)) {
            log.debug("Download for movie {} is already queued or running, ignoring the new request", movieId);
            return false;
        }

        try {
            torrentExecutor.execute(() -> runDownload(movieId, magnetUrl));
            return true;
        } catch (RuntimeException rejected) {
            // Queue full: let go of the claim so the movie can be asked for again.
            submitted.remove(movieId);
            log.error("Could not queue the download for movie {}", movieId, rejected);
            movieDownloadRepository.findByMovieId(movieId).ifPresent(download -> {
                download.setStatus(DownloadStatus.FAILED);
                movieDownloadRepository.save(download);
            });
            return false;
        }
    }

    private void runDownload(Long movieId, String magnetUrl) {
        TorrentRuntimePool.Lease lease = null;
        try {
            log.info("Starting background download for movie: {}", movieId);

            // This download's own runtime: a torrent started in a runtime that already
            // has one running never finds a peer (see TorrentRuntimePool).
            lease = runtimePool.acquire(movieId);
            BtRuntime downloadRuntime = lease.runtime();
            
            // --- STEP 1: Prepare the movie's download directory ---
            Path movieDir = Paths.get(videoStoragePath, String.valueOf(movieId));
            Files.createDirectories(movieDir);
            // --- STEP 2: Configure & Build BtClient ---
            Storage storage = new FileSystemStorage(movieDir);
            SequentialPieceSelector selector = new SequentialPieceSelector();
            selectors.put(movieId, selector);
            BtClient client = Bt.client(lease.runtime())
                .storage(storage)
                .magnet(magnetUrl)
                .selector(selector)
                .stopWhenDownloaded()
                .build();
            
            // --- STEP 3: Register Client & Update DB to DOWNLOADING ---
            activeClients.put(movieId, client);
            movieDownloadRepository.findByMovieId(movieId).ifPresent(download -> {
                download.setStatus(DownloadStatus.DOWNLOADING);
                download.setStoragePath(movieDir.toAbsolutePath().toString());
                download.setStartedAt(LocalDateTime.now());
                movieDownloadRepository.save(download);
            });

                // --- STEP 4: Heartbeat Loop (Updates every 1 second) ---
                AtomicLong previousDownloaded = new AtomicLong(0);
                AtomicBoolean readyToStreamMarked = new AtomicBoolean(false);

                // bt asks the DHT for peers on its own once a torrent's metadata is known,
                // and periodically per runtime. Nudging it while nothing is coming in gets
                // a fresh runtime past its DHT bootstrap sooner and revives a swarm that
                // has gone quiet.
                IPeerRegistry peerRegistry = lease.runtime().service(IPeerRegistry.class);
                TorrentId torrentId = torrentIdOf(magnetUrl);
                runtimes.put(movieId, lease.runtime());
                if (torrentId != null) {
                    torrentIds.put(movieId, torrentId);
                }
                AtomicBoolean videoFileLayoutConfigured = new AtomicBoolean(false);
                AtomicLong lastPeerTrigger = new AtomicLong(0);
                AtomicLong lastProgressPersist = new AtomicLong(System.currentTimeMillis());

                CompletableFuture<?> future = client.startAsync(sessionState -> {
                    Long downloaded = sessionState.getDownloaded();
                    Long left = sessionState.getLeft();
                    // A negative "left" means the metadata has not been fetched yet, so
                    // the real size is still unknown - reporting it as -1 would make the
                    // progress maths nonsense.
                    boolean metadataKnown = left >= 0;
                    Long total = metadataKnown ? downloaded + left : 0L;

                    // A magnet URI does not reliably include the file extension. Once
                    // metadata is available, map the selected video's real torrent byte
                    // range, then prioritize its header and (for MP4) its trailing index.
                    if (metadataKnown && torrentId != null && !videoFileLayoutConfigured.get()) {
                        if (configureVideoFileLayout(downloadRuntime, torrentId, selector, movieId)) {
                            videoFileLayoutConfigured.set(true);
                        }
                    }

                    // 1. Calculate speed: (bytes now - bytes 1 second ago)
                    Long prev = previousDownloaded.get();
                    Long speed = downloaded - prev;
                    // safety check
                    if (speed < 0) {
                        speed = 0l;
                    }
                    previousDownloaded.set(downloaded);

                    // 1b. No metadata yet, or no bytes moving: go looking for peers.
                    if (torrentId != null && (!metadataKnown || speed == 0)) {
                        long now = System.currentTimeMillis();
                        if (now - lastPeerTrigger.get() >= PEER_TRIGGER_INTERVAL_MS) {
                            lastPeerTrigger.set(now);
                            peerRegistry.triggerPeerCollection(torrentId);
                            log.info("Movie {}: {} - asking the DHT for peers again", movieId,
                                    metadataKnown ? "no data coming in" : "still waiting for the torrent metadata");
                        }
                    }

                    // 2. Calculate percentage (0.0 to 100.0)
                    double progress = 0.0;
                    if (total > 0) {
                        progress = ((double) downloaded / total) * 100.0;
                    }

                    // 3. Check the buffer threshold and every piece ffprobe needs. For
                    // non-faststart MP4s this includes the trailing moov/index pieces.
                    boolean probeDataReady = isInitialProbeDataDownloaded(movieId).orElse(false);
                    boolean streamReady = false;
                    if ((downloaded >= 8 * 1024 * 1024 && probeDataReady)
                            || (total > 0 && downloaded.equals(total))) {
                        streamReady = true;
                    }

                    if (streamReady && !readyToStreamMarked.get()) {
                        readyToStreamMarked.set(true);
                        log.info("Movie {} reached the initial streaming threshold.", movieId);

                        movieDownloadRepository.findByMovieId(movieId).ifPresent(download -> {
                            download.setStatus(DownloadStatus.READY_TO_STREAM);
                            download.setReadyToStream(true);
                            download.setTotalBytes(total);
                            movieDownloadRepository.save(download);
                        });
                    }

                    // 4. Determine status
                    DownloadStatus currentStatus;
                    if (metadataKnown && sessionState.getPiecesRemaining() == 0) {
                        currentStatus = DownloadStatus.COMPLETED;
                    } else if (readyToStreamMarked.get()) {
                        currentStatus = DownloadStatus.READY_TO_STREAM;
                    } else {
                        currentStatus = DownloadStatus.DOWNLOADING;
                    }

                    // 4b. The byte counters only lived in memory, so every answer served
                    // from the database after a restart claimed 0 bytes downloaded - and
                    // the streaming gate works out how much is playable from exactly those
                    // two numbers.
                    if (metadataKnown
                            && System.currentTimeMillis() - lastProgressPersist.get() >= PROGRESS_PERSIST_INTERVAL_MS) {
                        lastProgressPersist.set(System.currentTimeMillis());
                        Long persistedDownloaded = downloaded;
                        Long persistedTotal = total;
                        movieDownloadRepository.findByMovieId(movieId).ifPresent(download -> {
                            download.setDownloadedBytes(persistedDownloaded);
                            download.setTotalBytes(persistedTotal);
                            movieDownloadRepository.save(download);
                        });
                    }

                    // 5. Put in-memory snapshot for the frontend
                    progressCache.put(movieId, DownloadProgressDto.builder()
                        .movieId(movieId)
                        .status(currentStatus)
                        .downloadedBytes(downloaded)
                        .totalBytes(total)
                        .progressPercentage(progress)
                        .downloadSpeedBps(speed)
                        .isReadyToStream(streamReady)
                        .build());

                    if (speed > 0) {
                        log.info("Movie {}: Progress = {}% | Speed = {} KB/s", 
                                movieId, String.format("%.2f", progress), speed / 1024);
                    }
                }, 1000);

            // Wait here on the worker thread until download completes or is stopped
            future.join();

            // stopDownload() already took the client out, so the join returned on a
            // cancelled download - it is PAUSED, not finished.
            if (activeClients.remove(movieId) == null) {
                log.info("Download for movie {} was stopped before it finished", movieId);
                return;
            }

            // Download completed 100%!
            log.info("Download completed successfully for movie: {}", movieId);
            DownloadProgressDto finalProgress = progressCache.get(movieId);
            movieDownloadRepository.findByMovieId(movieId).ifPresent(download -> {
                download.setStatus(DownloadStatus.COMPLETED);
                download.setCompletedAt(LocalDateTime.now());
                // A COMPLETED row whose counters say otherwise is what makes a partly
                // downloaded film look playable from end to end.
                if (finalProgress != null && finalProgress.getTotalBytes() > 0) {
                    download.setTotalBytes(finalProgress.getTotalBytes());
                    download.setDownloadedBytes(finalProgress.getTotalBytes());
                }
                movieDownloadRepository.save(download);
            });
        } catch (Exception e) {
            log.error("Error starting download for movie: {}", movieId, e);
            activeClients.remove(movieId);
            progressCache.remove(movieId);
            movieDownloadRepository.findByMovieId(movieId).ifPresent(download -> {
                download.setStatus(DownloadStatus.FAILED);
                movieDownloadRepository.save(download);
            });
        } finally {
            // Frees the runtime's ports and the claim, so both can serve a retry or
            // another movie - the thread itself is already back in the pool.
            runtimePool.release(lease);
            selectors.remove(movieId);
            runtimes.remove(movieId);
            torrentIds.remove(movieId);
            videoFileLayouts.remove(movieId);
            submitted.remove(movieId);
        }
    }

    /**
     * Configures the selected video's torrent byte/piece range and playback priorities.
     * Returns false only while metadata or the data descriptor is unavailable, so the
     * heartbeat can retry.
     */
    boolean configureVideoFileLayout(BtRuntime runtime, TorrentId torrentId,
            SequentialPieceSelector selector, Long movieId) {
        try {
            TorrentRegistry registry = runtime.service(TorrentRegistry.class);
            Optional<Torrent> torrent = registry.getTorrent(torrentId);
            Optional<TorrentDescriptor> descriptor = registry.getDescriptor(torrentId);
            if (torrent.isEmpty() || descriptor.isEmpty()
                    || descriptor.get().getDataDescriptor() == null) {
                return false;
            }

            var files = torrent.get().getFiles();
            int videoIndex = -1;
            long largestVideoSize = -1;
            for (int i = 0; i < files.size(); i++) {
                TorrentFile file = files.get(i);
                if (isVideoFile(file) && file.getSize() > largestVideoSize) {
                    videoIndex = i;
                    largestVideoSize = file.getSize();
                }
            }

            selector.setIndexPieces(new BitSet());
            if (videoIndex < 0) {
                selector.setVideoPieces(new BitSet());
                videoFileLayouts.remove(movieId);
                log.debug("Movie {}: torrent metadata contains no video file", movieId);
                return true;
            }

            long fileOffset = 0;
            for (int i = 0; i < videoIndex; i++) {
                fileOffset = Math.addExact(fileOffset, files.get(i).getSize());
            }

            TorrentFile video = files.get(videoIndex);
            long pieceSize = torrent.get().getChunkSize();
            BitSet videoPieces = descriptor.get().getDataDescriptor()
                    .getAllPiecesForFiles(Set.of(video));
            if (videoPieces.isEmpty() || pieceSize <= 0 || video.getSize() <= 0) {
                return false;
            }

            BitSet videoPiecesCopy = (BitSet) videoPieces.clone();
            BitSet requiredProbePieces = new BitSet();
            requiredProbePieces.set(videoPieces.nextSetBit(0));
            selector.setVideoPieces(videoPiecesCopy);

            if (fileName(video).toLowerCase(Locale.ROOT).endsWith(".mp4")) {
                int lastVideoPiece = videoPieces.previousSetBit(videoPieces.length() - 1);
                int firstVideoPiece = videoPieces.nextSetBit(0);
                long piecesToPrioritize = Math.max(1L, MP4_INDEX_PREFETCH_BYTES / pieceSize
                        + (MP4_INDEX_PREFETCH_BYTES % pieceSize == 0 ? 0 : 1));
                int firstIndexPiece = (int) Math.max(firstVideoPiece,
                        (long) lastVideoPiece - piecesToPrioritize + 1);
                BitSet prioritizedPieces = (BitSet) videoPieces.clone();
                prioritizedPieces.clear(0, firstIndexPiece);
                selector.setIndexPieces(prioritizedPieces);
                // Prioritize the whole tail, but only gate on its final piece. ffprobe can
                // tell us whether the moov atom is readable; if it spans further back, the
                // stream retry lets the torrent fill those pieces without waiting for the
                // entire 32 MiB prefetch window first.
                requiredProbePieces.set(lastVideoPiece);
                int previousTailPiece = videoPieces.previousSetBit(lastVideoPiece - 1);
                if (previousTailPiece >= firstVideoPiece
                        && MP4_PROBE_TAIL_PIECES > 1) {
                    // One preceding piece allows small moov atoms crossing a piece boundary
                    // to be read on the first probe attempt.
                    requiredProbePieces.set(previousTailPiece);
                }
                log.info("Movie {}: prioritizing {} MP4 index piece(s) through piece {}",
                        movieId, prioritizedPieces.cardinality(), lastVideoPiece);
            }

            videoFileLayouts.put(movieId, new VideoFileLayout(
                    fileOffset, video.getSize(), pieceSize, videoPiecesCopy, requiredProbePieces));

            runtimes.put(movieId, runtime);
            torrentIds.put(movieId, torrentId);
            log.info("Movie {}: selected video {} spans bytes {}-{} and pieces {}-{}",
                    movieId, fileName(video), fileOffset, fileOffset + video.getSize() - 1,
                    videoPieces.nextSetBit(0), videoPieces.previousSetBit(videoPieces.length() - 1));
            return true;
        } catch (RuntimeException e) {
            log.debug("Could not configure video file layout for movie {} yet: {}", movieId, e.getMessage());
            return false;
        }
    }

    private record VideoFileLayout(long fileOffset, long fileSize, long pieceSize,
            BitSet videoPieces, BitSet requiredProbePieces) {
    }

    private boolean isVideoFile(TorrentFile file) {
        String name = fileName(file).toLowerCase(Locale.ROOT);
        int extensionSeparator = name.lastIndexOf('.');
        return extensionSeparator >= 0 && VIDEO_EXTENSIONS.contains(name.substring(extensionSeparator + 1));
    }

    private String fileName(TorrentFile file) {
        var path = file.getPathElements();
        return path.isEmpty() ? "" : path.get(path.size() - 1);
    }

    /** The info hash bt knows the torrent by, so we can drive its peer lookups. */
    private TorrentId torrentIdOf(String magnetUrl) {
        try {
            return MagnetUriParser.lenientParser().parse(magnetUrl).getTorrentId();
        } catch (Exception e) {
            log.warn("Could not read the info hash out of the magnet link: {}", e.getMessage());
            return null;
        }
    }
}
