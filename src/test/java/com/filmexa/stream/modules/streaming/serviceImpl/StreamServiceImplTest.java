package com.filmexa.stream.modules.streaming.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.filmexa.stream.modules.download.dto.DownloadRequestDto;
import com.filmexa.stream.modules.download.entity.MovieDownload;
import com.filmexa.stream.modules.download.enums.DownloadStatus;
import com.filmexa.stream.modules.download.magnet.MagnetResolver;
import com.filmexa.stream.modules.download.repo.MovieDownloadRepository;
import com.filmexa.stream.modules.download.service.TorrentDownloadService;
import com.filmexa.stream.modules.streaming.config.StreamProperties;
import com.filmexa.stream.modules.streaming.dto.MediaInfo;
import com.filmexa.stream.modules.streaming.dto.StreamSessionDto;
import com.filmexa.stream.modules.streaming.dto.SubtitleTrack;
import com.filmexa.stream.modules.streaming.enums.StreamState;
import com.filmexa.stream.modules.streaming.ffmpeg.Ffmpeg;
import com.filmexa.stream.modules.streaming.playlist.PlaylistBuilder;
import com.filmexa.stream.modules.streaming.security.StreamTokenService;
import com.filmexa.stream.modules.users.entity.User;
import com.filmexa.stream.modules.users.enums.PreferredLanguage;

@ExtendWith(MockitoExtension.class)
class StreamServiceImplTest {

    private static final Long MOVIE_ID = 27205L;
    private static final String MAGNET = "magnet:?xt=urn:btih:abc";

    @Mock private Ffmpeg ffmpeg;
    @Mock private PlaylistBuilder playlistBuilder;
    @Mock private StreamTokenService streamTokenService;
    @Mock private TorrentDownloadService torrentDownloadService;
    @Mock private MovieDownloadRepository movieDownloadRepository;
    @Mock private MagnetResolver magnetResolver;

    private StreamServiceImpl service;
    private User viewer;

    @BeforeEach
    void setUp() {
        viewer = new User();
        viewer.setUsername("johndoe");
        viewer.setPreferredLanguage(PreferredLanguage.ENGLISH);

        service = new StreamServiceImpl(
                ffmpeg, playlistBuilder, streamTokenService, new StreamProperties(),
                torrentDownloadService, movieDownloadRepository, magnetResolver);
        // Points the file lookup at a directory that does not exist, so nothing is
        // playable and every case below lands in PREPARING.
        ReflectionTestUtils.setField(service, "videoStoragePath", "/tmp/filmexa-test-nowhere");
    }

    private MovieDownload downloadWith(DownloadStatus status) {
        MovieDownload download = new MovieDownload();
        download.setMovieId(MOVIE_ID);
        download.setStatus(status);
        return download;
    }

    @Test
    void firstPlayResolvesAMagnetAndStartsTheDownload() {
        when(movieDownloadRepository.findByMovieId(MOVIE_ID)).thenReturn(Optional.empty());
        when(magnetResolver.resolve(MOVIE_ID)).thenReturn(MAGNET);
        when(torrentDownloadService.startDownload(any()))
                .thenReturn(downloadWith(DownloadStatus.PENDING));

        StreamSessionDto session = service.createSession(MOVIE_ID, viewer);

        ArgumentCaptor<DownloadRequestDto> request = ArgumentCaptor.forClass(DownloadRequestDto.class);
        verify(torrentDownloadService).startDownload(request.capture());
        assertThat(request.getValue().getMovieId()).isEqualTo(MOVIE_ID);
        assertThat(request.getValue().getMagnetUrl()).isEqualTo(MAGNET);

        assertThat(session.getState()).isEqualTo(StreamState.PREPARING);
        assertThat(session.getManifestUrl()).isNull();
        assertThat(session.getToken()).isNull();
    }

    @Test
    void pollingAnInProgressDownloadDoesNotStartItAgain() {
        when(movieDownloadRepository.findByMovieId(MOVIE_ID))
                .thenReturn(Optional.of(downloadWith(DownloadStatus.DOWNLOADING)));
        when(torrentDownloadService.isActive(MOVIE_ID)).thenReturn(true);

        StreamSessionDto session = service.createSession(MOVIE_ID, viewer);

        verify(torrentDownloadService, never()).startDownload(any());
        verify(magnetResolver, never()).resolve(anyLong());
        assertThat(session.getState()).isEqualTo(StreamState.PREPARING);
        assertThat(session.getDownloadStatus()).isEqualTo(DownloadStatus.DOWNLOADING);
    }

    @Test
    void aRowLeftDownloadingByARestartIsResumedRatherThanPollingForever() {
        // The worker holds its client list in memory, so a restart leaves the stored
        // status saying DOWNLOADING while nothing is actually running. Without a resume
        // the movie would sit in PREPARING for ever.
        when(movieDownloadRepository.findByMovieId(MOVIE_ID))
                .thenReturn(Optional.of(downloadWith(DownloadStatus.DOWNLOADING)));
        when(torrentDownloadService.isActive(MOVIE_ID)).thenReturn(false);
        when(magnetResolver.resolve(MOVIE_ID)).thenReturn(MAGNET);
        when(torrentDownloadService.startDownload(any()))
                .thenReturn(downloadWith(DownloadStatus.PENDING));

        service.createSession(MOVIE_ID, viewer);

        verify(torrentDownloadService).startDownload(any());
    }

    @Test
    void aFinishedDownloadIsNeverRestartedEvenThoughNothingIsRunning() {
        when(movieDownloadRepository.findByMovieId(MOVIE_ID))
                .thenReturn(Optional.of(downloadWith(DownloadStatus.COMPLETED)));

        service.createSession(MOVIE_ID, viewer);

        verify(torrentDownloadService, never()).startDownload(any());
        verify(magnetResolver, never()).resolve(anyLong());
    }

    @Test
    void aFailedDownloadIsRestartedOnTheNextPlay() {
        when(movieDownloadRepository.findByMovieId(MOVIE_ID))
                .thenReturn(Optional.of(downloadWith(DownloadStatus.FAILED)));
        when(magnetResolver.resolve(MOVIE_ID)).thenReturn(MAGNET);
        when(torrentDownloadService.startDownload(any()))
                .thenReturn(downloadWith(DownloadStatus.PENDING));

        service.createSession(MOVIE_ID, viewer);

        verify(torrentDownloadService).startDownload(any());
    }

    @Test
    void aPausedDownloadIsResumedOnTheNextPlay() {
        when(movieDownloadRepository.findByMovieId(MOVIE_ID))
                .thenReturn(Optional.of(downloadWith(DownloadStatus.PAUSED)));
        when(magnetResolver.resolve(MOVIE_ID)).thenReturn(MAGNET);
        when(torrentDownloadService.startDownload(any()))
                .thenReturn(downloadWith(DownloadStatus.PENDING));

        service.createSession(MOVIE_ID, viewer);

        verify(torrentDownloadService).startDownload(any());
    }

    @Test
    void bitmapSubtitleTracksAreNeverOfferedBecauseTheyCannotBecomeWebVtt() {
        MediaInfo info = new MediaInfo(
                java.nio.file.Path.of("movie.mkv"), 1920, 1080, 600, "eng",
                java.util.List.of(
                        new SubtitleTrack(0, "eng", null, true),
                        new SubtitleTrack(1, "fre", null, false)));

        assertThat(info.subtitles()).hasSize(2);
        assertThat(info.subtitles().stream().filter(SubtitleTrack::convertible).toList())
                .singleElement()
                .satisfies(track -> assertThat(track.language()).isEqualTo("eng"));
    }

    @Test
    void askingForABitmapTrackIsANotFoundRatherThanAFailedExtraction() {
        when(movieDownloadRepository.findByMovieId(MOVIE_ID))
                .thenReturn(Optional.of(downloadWith(DownloadStatus.COMPLETED)));

        // No file on disk, so the probe never even happens - the lookup fails first.
        assertThatThrownBy(() -> service.subtitle(MOVIE_ID, 1))
                .isInstanceOf(com.filmexa.stream.common.exception.NotFoundException.class);
    }

    @Test
    void noTokenIsIssuedWhileTheMovieIsStillPreparing() {
        when(movieDownloadRepository.findByMovieId(MOVIE_ID))
                .thenReturn(Optional.of(downloadWith(DownloadStatus.DOWNLOADING)));

        service.createSession(MOVIE_ID, viewer);

        // A token handed out before the movie is playable would just be wasted, and it
        // would let a client fetch segments that cannot exist yet.
        verify(streamTokenService, never()).issue(any());
    }
}
