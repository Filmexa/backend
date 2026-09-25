package com.filmexa.stream.modules.download.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.filmexa.stream.modules.download.dto.DownloadRequestDto;
import com.filmexa.stream.modules.download.entity.MovieDownload;
import com.filmexa.stream.modules.download.enums.DownloadStatus;
import com.filmexa.stream.modules.download.repo.MovieDownloadRepository;
import com.filmexa.stream.modules.download.worker.TorrentDownloadWorker;

@ExtendWith(MockitoExtension.class)
class TorrentDownloadServiceImplTest {

    private static final String MAGNET = "magnet:?xt=urn:btih:0123456789abcdef";

    @Mock
    private MovieDownloadRepository movieDownloadRepository;

    @Mock
    private TorrentDownloadWorker torrentDownloadWorker;

    private TorrentDownloadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TorrentDownloadServiceImpl(movieDownloadRepository, torrentDownloadWorker);
    }

    @Test
    void startDownloadRestartsStaleDownloadingRowAfterProcessRestart() {
        long movieId = 101L;
        MovieDownload existing = download(movieId, DownloadStatus.DOWNLOADING);
        existing.setReadyToStream(true);
        when(movieDownloadRepository.findByMovieId(movieId)).thenReturn(Optional.of(existing));
        when(torrentDownloadWorker.isActive(movieId)).thenReturn(false);

        MovieDownload result = service.startDownload(request(movieId));

        assertThat(result.getStatus()).isEqualTo(DownloadStatus.PENDING);
        assertThat(result.isReadyToStream()).isFalse();
        verify(torrentDownloadWorker).startDownloadAsync(movieId, MAGNET);
    }

    @Test
    void restartDownloadResetsIncompleteCompletedRowAndPreservesItsDownloadedBytes() {
        long movieId = 202L;
        MovieDownload existing = download(movieId, DownloadStatus.COMPLETED);
        existing.setDownloadedBytes(4_000_000L);
        existing.setCompletedAt(LocalDateTime.now());
        existing.setReadyToStream(true);
        when(movieDownloadRepository.findByMovieId(movieId)).thenReturn(Optional.of(existing));
        when(torrentDownloadWorker.isActive(movieId)).thenReturn(false);
        when(movieDownloadRepository.save(existing)).thenReturn(existing);

        MovieDownload result = service.restartDownload(request(movieId));

        assertThat(result.getStatus()).isEqualTo(DownloadStatus.PENDING);
        assertThat(result.isReadyToStream()).isFalse();
        assertThat(result.getCompletedAt()).isNull();
        assertThat(result.getDownloadedBytes()).isEqualTo(4_000_000L);
        verify(torrentDownloadWorker).startDownloadAsync(movieId, MAGNET);
    }

    @Test
    void restartDownloadDoesNotResetAnAlreadyActiveMovie() {
        long movieId = 303L;
        MovieDownload existing = download(movieId, DownloadStatus.READY_TO_STREAM);
        existing.setReadyToStream(true);
        when(movieDownloadRepository.findByMovieId(movieId)).thenReturn(Optional.of(existing));
        when(torrentDownloadWorker.isActive(movieId)).thenReturn(true);

        MovieDownload result = service.restartDownload(request(movieId));

        assertThat(result.getStatus()).isEqualTo(DownloadStatus.READY_TO_STREAM);
        assertThat(result.isReadyToStream()).isTrue();
        verify(movieDownloadRepository, never()).save(any(MovieDownload.class));
        verify(torrentDownloadWorker, never()).startDownloadAsync(movieId, MAGNET);
    }

    @Test
    void twoDifferentMoviesCanBeQueuedIndependently() {
        long firstMovieId = 101L;
        long secondMovieId = 202L;
        when(movieDownloadRepository.findByMovieId(firstMovieId)).thenReturn(Optional.empty());
        when(movieDownloadRepository.findByMovieId(secondMovieId)).thenReturn(Optional.empty());

        service.startDownload(request(firstMovieId));
        service.startDownload(request(secondMovieId));

        verify(torrentDownloadWorker).startDownloadAsync(firstMovieId, MAGNET);
        verify(torrentDownloadWorker).startDownloadAsync(secondMovieId, MAGNET);
    }

    private MovieDownload download(long movieId, DownloadStatus status) {
        MovieDownload download = new MovieDownload();
        download.setMovieId(movieId);
        download.setMagnetUrl(MAGNET);
        download.setStatus(status);
        return download;
    }

    private DownloadRequestDto request(long movieId) {
        DownloadRequestDto request = new DownloadRequestDto();
        request.setMovieId(movieId);
        request.setMagnetUrl(MAGNET);
        return request;
    }
}
