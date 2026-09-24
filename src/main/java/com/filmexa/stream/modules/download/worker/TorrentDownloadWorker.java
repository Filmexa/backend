package com.filmexa.stream.modules.download.worker;

import org.springframework.stereotype.Component;
import com.filmexa.stream.modules.download.dto.DownloadProgressDto;
import com.filmexa.stream.modules.download.repo.MovieDownloadRepository;
import com.filmexa.stream.modules.download.selector.SequentialPieceSelector;
import bt.Bt;
import bt.data.Storage;
import bt.data.file.FileSystemStorage;
import bt.runtime.BtClient;
import bt.runtime.BtRuntime;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import com.filmexa.stream.modules.download.enums.DownloadStatus;
import java.time.LocalDateTime;
import bt.runtime.Config;
import java.net.DatagramSocket;
import java.net.InetAddress;

@Component
@Slf4j
public class TorrentDownloadWorker {
    private final MovieDownloadRepository movieDownloadRepository;
    @Value("${app.video-storage-path:./data/movies}")
    private String videoStoragePath; 
    private final Map<Long, BtClient> activeClients = new ConcurrentHashMap<>(); 
    private final Map<Long, DownloadProgressDto> progressCache = new ConcurrentHashMap<>();

    private final BtRuntime btRuntime;

    public TorrentDownloadWorker(MovieDownloadRepository movieDownloadRepository, BtRuntime btRuntime) {
        this.movieDownloadRepository = movieDownloadRepository;
        this.btRuntime = btRuntime;
    }

    public DownloadProgressDto getProgress(Long movieId) {
        return progressCache.get(movieId);
    }

    public boolean isActive(Long movieId) {
        return activeClients.containsKey(movieId);
    }

    public void stopDownload(Long movieId) {
        BtClient client = activeClients.remove(movieId);
        if (client != null) {   
            client.stop();
            progressCache.remove(movieId);
            log.info("Stopped download for movie with ID: {}", movieId);
            movieDownloadRepository.findByMovieId(movieId).ifPresent(download -> {
                download.setStatus(DownloadStatus.PAUSED);
                movieDownloadRepository.save(download);
            });
        }
    }
    @Async("torrentTaskExecutor")
    public void startDownloadAsync(Long movieId, String magnetUrl) {
        try {
            log.info("Starting background download for movie: {}", movieId);
            
            // --- STEP 1: Directory & Format Detection ---
            Path movieDir = Paths.get(videoStoragePath, String.valueOf(movieId));
            Files.createDirectories(movieDir);
            boolean isMp4 = magnetUrl.toLowerCase().contains(".mp4");
            
            // --- STEP 2: Configure & Build BtClient ---
            Storage storage = new FileSystemStorage(movieDir);
            SequentialPieceSelector selector = new SequentialPieceSelector(isMp4);
            BtClient client = Bt.client(btRuntime)
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

                CompletableFuture<?> future = client.startAsync(sessionState -> {
                    Long downloaded = sessionState.getDownloaded();
                    Long left = sessionState.getLeft();
                    Long total = downloaded + left;

                    // 1. Calculate speed: (bytes now - bytes 1 second ago)
                    Long prev = previousDownloaded.get();
                    Long speed = downloaded - prev;
                    // safety check
                    if (speed < 0) {
                        speed = 0l;
                    }
                    previousDownloaded.set(downloaded);

                    // 2. Calculate percentage (0.0 to 100.0)
                    double progress = 0.0;
                    if (total > 0) {
                        progress = ((double) downloaded / total) * 100.0;
                    }

                    // 3. 42 Rule: Check if 10 MB buffer reached
                    boolean streamReady = false;
                    if (downloaded >= 10 * 1024 * 1024) {
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
                    if (sessionState.getPiecesRemaining() == 0) {
                        currentStatus = DownloadStatus.COMPLETED;
                    } else if (readyToStreamMarked.get()) {
                        currentStatus = DownloadStatus.READY_TO_STREAM;
                    } else {
                        currentStatus = DownloadStatus.DOWNLOADING;
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

            // Download completed 100%!
            activeClients.remove(movieId);
            log.info("Download completed successfully for movie: {}", movieId);
            movieDownloadRepository.findByMovieId(movieId).ifPresent(download -> {
                download.setStatus(DownloadStatus.COMPLETED);
                download.setCompletedAt(LocalDateTime.now());
                movieDownloadRepository.save(download);
            });
        } catch (Exception e) {
            log.error("Error starting download for movie: {}", movieId, e);
            movieDownloadRepository.findByMovieId(movieId).ifPresent(download -> {
                download.setStatus(DownloadStatus.FAILED);
                movieDownloadRepository.save(download);
                activeClients.remove(movieId);
                progressCache.remove(movieId);
            });
        }
    }

    private InetAddress getOutboundAddress() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress();
        } catch (Exception e) {
            log.warn("Could not determine outbound network interface: {}", e.getMessage());
            return null;
        }
    }
}
