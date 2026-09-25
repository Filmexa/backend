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
import bt.runtime.BtRuntime;
import bt.torrent.TorrentDescriptor;
import bt.torrent.TorrentRegistry;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.filmexa.stream.modules.download.enums.DownloadStatus;
import java.time.LocalDateTime;

@Component
@Slf4j
public class TorrentDownloadWorker {

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
            int total = bitfield.getPiecesTotal();
            if (total <= 0) {
                return Optional.empty();
            }

            int first = pieceAt(fromFraction, total);
            int last = pieceAt(toFraction, total);
            for (int piece = first; piece <= last; piece++) {
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

    private int pieceAt(double fraction, int totalPieces) {
        double clamped = Math.min(1.0, Math.max(0.0, fraction));
        return Math.min(totalPieces - 1, (int) (clamped * totalPieces));
    }

    public void stopDownload(Long movieId) {
        BtClient client = activeClients.remove(movieId);
        if (client != null) {   
            client.stop();
            progressCache.remove(movieId);
            selectors.remove(movieId);
            runtimes.remove(movieId);
            torrentIds.remove(movieId);
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
            
            // --- STEP 1: Directory & Format Detection ---
            Path movieDir = Paths.get(videoStoragePath, String.valueOf(movieId));
            Files.createDirectories(movieDir);
            boolean isMp4 = magnetUrl.toLowerCase().contains(".mp4");
            
            // --- STEP 2: Configure & Build BtClient ---
            Storage storage = new FileSystemStorage(movieDir);
            SequentialPieceSelector selector = new SequentialPieceSelector(isMp4);
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

                    // 3. 42 Rule: Check if 10 MB buffer reached
                    boolean streamReady = false;
                    if (downloaded >= 25 * 1024 * 1024) {
                        streamReady = true;
                    }

                    if (streamReady && !readyToStreamMarked.get()) {
                        readyToStreamMarked.set(true);
                        log.info("Movie {} reached 10 MB buffer! Ready to stream.", movieId);

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
            submitted.remove(movieId);
        }
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
