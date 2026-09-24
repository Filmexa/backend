package com.filmexa.stream.modules.download.scheduler;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import com.filmexa.stream.modules.download.entity.MovieDownload;
import com.filmexa.stream.modules.download.repo.MovieDownloadRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class MovieCleanupScheduler {

    private final MovieDownloadRepository movieDownloadRepository;

    // Runs every day at 3:00 AM
    @Scheduled(cron = "${app.cleanup.cron:0 0 3 * * ?}")
    public void cleanupUnwatchedMovies() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        log.info("Running 30-day unwatched movies cleanup. Cutoff: {}", cutoff);

        List<MovieDownload> expiredMovies = movieDownloadRepository.findByLastWatchedAtBefore(cutoff);

        for (MovieDownload movie : expiredMovies) {
            deleteMovieFiles(movie);
            movieDownloadRepository.delete(movie);
            log.info("Purged unwatched movie ID: {} from disk and database", movie.getMovieId());
        }
    }

    private void deleteMovieFiles(MovieDownload movie) {
        if (movie.getStoragePath() != null) {
            try {
                Path path = Paths.get(movie.getStoragePath());
                FileSystemUtils.deleteRecursively(path);
                log.info("Deleted files on disk for movie: {}", movie.getMovieId());
            } catch (IOException e) {
                log.error("Failed to delete files for movie: {}", movie.getMovieId(), e);
            }
        }
    }
}
