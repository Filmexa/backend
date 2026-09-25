package com.filmexa.stream.modules.streaming.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.filmexa.stream.modules.download.dto.DownloadProgressDto;
import com.filmexa.stream.modules.download.dto.DownloadRequestDto;
import com.filmexa.stream.modules.download.entity.MovieDownload;
import com.filmexa.stream.modules.download.enums.DownloadStatus;
import com.filmexa.stream.modules.download.repo.MovieDownloadRepository;
import com.filmexa.stream.modules.download.service.TorrentDownloadService;
import com.filmexa.stream.modules.moviesExternal.client.MovieProvider;
import com.filmexa.stream.modules.streaming.config.StreamProperties;
import com.filmexa.stream.modules.streaming.dto.StreamSessionDto;
import com.filmexa.stream.modules.streaming.enums.StreamState;
import com.filmexa.stream.modules.streaming.exception.StreamNotReadyException;
import com.filmexa.stream.modules.streaming.ffmpeg.Ffmpeg;
import com.filmexa.stream.modules.streaming.playlist.PlaylistBuilder;
import com.filmexa.stream.modules.streaming.security.StreamTokenService;
import com.filmexa.stream.modules.torrent.dto.TorrentResultDto;
import com.filmexa.stream.modules.torrent.service.TorrentService;

@ExtendWith(MockitoExtension.class)
class StreamServiceImplTest {

    @Mock private Ffmpeg ffmpeg;
    @Mock private PlaylistBuilder playlistBuilder;
    @Mock private StreamTokenService streamTokenService;
    @Mock private TorrentDownloadService torrentDownloadService;
    @Mock private MovieDownloadRepository movieDownloadRepository;
    @Mock private TorrentService torrentService;
    @Mock private MovieProvider movieProvider;

    @TempDir Path tempDir;

    private StreamServiceImpl streamService;

    @BeforeEach
    void setUp() {
        streamService = new StreamServiceImpl(ffmpeg, playlistBuilder, streamTokenService,
                new StreamProperties(), torrentDownloadService, movieDownloadRepository,
                torrentService, movieProvider);
        ReflectionTestUtils.setField(streamService, "videoStoragePath", "unused-in-preparing-test");
    }

    @Test
    void twoMoviesRequestedAtTheSameTimeStartIndependentDownloads() throws Exception {
        long firstMovieId = 101L;
        long secondMovieId = 202L;
        DownloadProgressDto pending = DownloadProgressDto.builder()
                .status(DownloadStatus.PENDING)
                .isReadyToStream(false)
                .build();
        TorrentResultDto torrent = mock(TorrentResultDto.class);
        when(torrent.getMagnet()).thenReturn("magnet:?xt=urn:btih:0123456789abcdef");
        when(movieDownloadRepository.findByMovieId(anyLong())).thenReturn(Optional.empty());
        when(torrentService.resolve(anyString())).thenReturn(Optional.of(torrent));
        when(torrentDownloadService.getProgress(anyLong())).thenReturn(pending);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<StreamSessionDto> first = executor.submit(() -> {
                ready.countDown();
                start.await();
                return streamService.createSession(firstMovieId, "tt0000101", null);
            });
            Future<StreamSessionDto> second = executor.submit(() -> {
                ready.countDown();
                start.await();
                return streamService.createSession(secondMovieId, "tt0000202", null);
            });

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS).getState()).isEqualTo(StreamState.PREPARING);
            assertThat(second.get(5, TimeUnit.SECONDS).getState()).isEqualTo(StreamState.PREPARING);
        } finally {
            executor.shutdownNow();
        }

        ArgumentCaptor<DownloadRequestDto> requests = ArgumentCaptor.forClass(DownloadRequestDto.class);
        verify(torrentDownloadService, times(2)).startDownload(requests.capture());
        assertThat(requests.getAllValues())
                .extracting(DownloadRequestDto::getMovieId)
                .containsExactlyInAnyOrder(firstMovieId, secondMovieId);
    }

    @Test
    void staleDownloadingRowUsesTheRestartPath() {
        long movieId = 404L;
        MovieDownload stale = new MovieDownload();
        stale.setMovieId(movieId);
        stale.setStatus(DownloadStatus.DOWNLOADING);
        TorrentResultDto torrent = mock(TorrentResultDto.class);
        when(torrent.getMagnet()).thenReturn("magnet:?xt=urn:btih:0123456789abcdef");
        when(movieDownloadRepository.findByMovieId(movieId)).thenReturn(Optional.of(stale));
        when(torrentDownloadService.isActive(movieId)).thenReturn(false);
        when(torrentService.resolve("tt0000404")).thenReturn(Optional.of(torrent));
        when(torrentDownloadService.restartDownload(any(DownloadRequestDto.class))).thenReturn(stale);
        when(torrentDownloadService.getProgress(movieId)).thenReturn(DownloadProgressDto.builder()
                .status(DownloadStatus.PENDING)
                .isReadyToStream(false)
                .build());

        StreamSessionDto session = streamService.createSession(movieId, "tt0000404", null);

        assertThat(session.getState()).isEqualTo(StreamState.PREPARING);
        verify(torrentDownloadService).restartDownload(org.mockito.ArgumentMatchers.argThat(
                request -> request.getMovieId().equals(movieId)));
        verify(torrentDownloadService, never()).startDownload(any(DownloadRequestDto.class));
    }

    @Test
    void incompleteMovieProbeIsThrottledWhileMorePiecesArrive() throws Exception {
        long movieId = 505L;
        Path movieDirectory = Files.createDirectory(tempDir.resolve(String.valueOf(movieId)));
        Path partialMovie = Files.createFile(movieDirectory.resolve("partial.mp4"));
        ReflectionTestUtils.setField(streamService, "videoStoragePath", tempDir.toString());
        when(ffmpeg.probe(any())).thenThrow(
                new StreamNotReadyException("Not enough of this movie has downloaded yet", 10));

        assertThatThrownBy(() -> streamService.mediaInfo(movieId))
                .isInstanceOf(StreamNotReadyException.class)
                .hasMessage("Not enough of this movie has downloaded yet");
        assertThatThrownBy(() -> streamService.mediaInfo(movieId))
                .isInstanceOf(StreamNotReadyException.class)
                .hasMessage("Waiting for more of the movie file to download");

        verify(ffmpeg, times(1)).probe(partialMovie.toAbsolutePath());
    }
}
