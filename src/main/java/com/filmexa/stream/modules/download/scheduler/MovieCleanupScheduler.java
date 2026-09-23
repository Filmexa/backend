package com.filmexa.stream.modules.download.scheduler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.filmexa.stream.modules.download.entity.MovieDownload;
import com.filmexa.stream.modules.download.repo.MovieDownloadRepository;
import com.filmexa.stream.modules.download.service.TorrentDownloadService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Deletes movies nobody has watched for a month.
 *
 * <p>Everything for a movie lives under one directory - the downloaded file, the extracted
 * subtitles - so cleanup is "stop the torrent, remove the folder, drop the row".
 * {@code lastWatchedAt} is refreshed every time a play session or a manifest is requested,
 * so a movie being watched right now can never look stale.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MovieCleanupScheduler {

    private final MovieDownloadRepository movieDownloadRepository;
    private final TorrentDownloadService torrentDownloadService;

    @Value("${app.video-storage-path:./data/movies}")
    private String videoStoragePath;

    @Value("${app.cleanup.retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "${app.cleanup.cron:0 0 3 * * *}")
    public void deleteUnwatchedMovies() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<MovieDownload> stale = movieDownloadRepository.findByLastWatchedAtBefore(cutoff);

        if (stale.isEmpty()) {
            log.debug("Cleanup: nothing unwatched since {}", cutoff);
            return;
        }

        log.info("Cleanup: removing {} movie(s) unwatched since {}", stale.size(), cutoff);
        for (MovieDownload download : stale) {
            remove(download);
        }
    }

    private void remove(MovieDownload download) {
        Long movieId = download.getMovieId();
        try {
            // A download can still be running if nobody ever came back to watch it.
            // Stop it first, or the torrent client keeps writing into the folder we
            // are about to delete.
            torrentDownloadService.stopDownload(movieId);

            deleteDirectory(Paths.get(videoStoragePath, String.valueOf(movieId)).toAbsolutePath());
            movieDownloadRepository.delete(download);

            log.info("Cleanup: removed movie {} (last watched {})", movieId, download.getLastWatchedAt());
        } catch (Exception e) {
            // One bad movie must not stop the rest of the sweep.
            log.error("Cleanup: could not remove movie {}", movieId, e);
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(directory)) {
            // Deepest first, so directories are empty by the time we reach them.
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    log.warn("Cleanup: could not delete {}: {}", path, e.getMessage());
                }
            });
        }
    }
}
