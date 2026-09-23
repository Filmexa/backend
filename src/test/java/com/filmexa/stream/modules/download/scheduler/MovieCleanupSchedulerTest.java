package com.filmexa.stream.modules.download.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.filmexa.stream.modules.download.entity.MovieDownload;
import com.filmexa.stream.modules.download.repo.MovieDownloadRepository;
import com.filmexa.stream.modules.download.service.TorrentDownloadService;

@ExtendWith(MockitoExtension.class)
class MovieCleanupSchedulerTest {

    private static final Long MOVIE_ID = 27205L;

    @Mock private MovieDownloadRepository movieDownloadRepository;
    @Mock private TorrentDownloadService torrentDownloadService;

    private MovieCleanupScheduler scheduler;
    private Path storageRoot;
    private Path movieDirectory;

    @BeforeEach
    void setUp(@org.junit.jupiter.api.io.TempDir Path tempDir) throws IOException {
        storageRoot = tempDir;
        scheduler = new MovieCleanupScheduler(movieDownloadRepository, torrentDownloadService);
        ReflectionTestUtils.setField(scheduler, "videoStoragePath", storageRoot.toString());
        ReflectionTestUtils.setField(scheduler, "retentionDays", 30);

        // A movie folder as it looks in practice: the video plus extracted subtitles.
        movieDirectory = storageRoot.resolve(String.valueOf(MOVIE_ID));
        Files.createDirectories(movieDirectory.resolve("subtitles"));
        Files.writeString(movieDirectory.resolve("movie.mkv"), "video bytes");
        Files.writeString(movieDirectory.resolve("subtitles").resolve("0.vtt"), "WEBVTT");
    }

    private MovieDownload staleDownload() {
        MovieDownload download = new MovieDownload();
        download.setMovieId(MOVIE_ID);
        download.setLastWatchedAt(LocalDateTime.now().minusDays(40));
        return download;
    }

    @Test
    void anUnwatchedMovieLosesItsFilesAndItsRow() {
        MovieDownload stale = staleDownload();
        when(movieDownloadRepository.findByLastWatchedAtBefore(any())).thenReturn(List.of(stale));

        scheduler.deleteUnwatchedMovies();

        assertThat(movieDirectory).doesNotExist();
        verify(movieDownloadRepository).delete(stale);
    }

    @Test
    void theTorrentIsStoppedBeforeItsFolderIsDeleted() {
        when(movieDownloadRepository.findByLastWatchedAtBefore(any()))
                .thenReturn(List.of(staleDownload()));

        scheduler.deleteUnwatchedMovies();

        // Otherwise the client keeps writing into a directory we just removed.
        verify(torrentDownloadService).stopDownload(MOVIE_ID);
    }

    @Test
    void theCutoffIsRetentionDaysBackNotSomeOtherWindow() {
        when(movieDownloadRepository.findByLastWatchedAtBefore(any())).thenReturn(List.of());

        LocalDateTime before = LocalDateTime.now().minusDays(30);
        scheduler.deleteUnwatchedMovies();
        LocalDateTime after = LocalDateTime.now().minusDays(30);

        org.mockito.ArgumentCaptor<LocalDateTime> cutoff =
                org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        verify(movieDownloadRepository).findByLastWatchedAtBefore(cutoff.capture());
        assertThat(cutoff.getValue()).isBetween(before, after);
    }

    @Test
    void nothingIsDeletedWhenEveryMovieIsRecent() {
        when(movieDownloadRepository.findByLastWatchedAtBefore(any())).thenReturn(List.of());

        scheduler.deleteUnwatchedMovies();

        assertThat(movieDirectory).exists();
        verify(movieDownloadRepository, never()).delete(any());
        verify(torrentDownloadService, never()).stopDownload(anyLong());
    }

    @Test
    void oneUndeletableMovieDoesNotAbortTheWholeSweep() {
        MovieDownload missingFolder = new MovieDownload();
        missingFolder.setMovieId(99999L);
        missingFolder.setLastWatchedAt(LocalDateTime.now().minusDays(40));
        MovieDownload stale = staleDownload();

        when(movieDownloadRepository.findByLastWatchedAtBefore(any()))
                .thenReturn(List.of(missingFolder, stale));

        scheduler.deleteUnwatchedMovies();

        // The second movie is still processed even though the first had no folder.
        assertThat(movieDirectory).doesNotExist();
        verify(movieDownloadRepository).delete(stale);
    }
}
