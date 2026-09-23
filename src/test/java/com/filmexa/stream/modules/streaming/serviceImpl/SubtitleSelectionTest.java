package com.filmexa.stream.modules.streaming.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.filmexa.stream.modules.download.entity.MovieDownload;
import com.filmexa.stream.modules.download.enums.DownloadStatus;
import com.filmexa.stream.modules.download.magnet.MagnetResolver;
import com.filmexa.stream.modules.download.repo.MovieDownloadRepository;
import com.filmexa.stream.modules.download.service.TorrentDownloadService;
import com.filmexa.stream.modules.streaming.config.StreamProperties;
import com.filmexa.stream.modules.streaming.dto.MediaInfo;
import com.filmexa.stream.modules.streaming.dto.StreamSessionDto;
import com.filmexa.stream.modules.streaming.dto.SubtitleTrack;
import com.filmexa.stream.modules.streaming.dto.SubtitleTrackDto;
import com.filmexa.stream.modules.streaming.enums.StreamState;
import com.filmexa.stream.modules.streaming.ffmpeg.Ffmpeg;
import com.filmexa.stream.modules.streaming.playlist.PlaylistBuilder;
import com.filmexa.stream.modules.streaming.security.StreamTokenService;
import com.filmexa.stream.modules.users.entity.User;
import com.filmexa.stream.modules.users.enums.PreferredLanguage;

/**
 * The subject's rule: subtitles come up automatically when the movie's audio is not in
 * the viewer's language.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubtitleSelectionTest {

    private static final Long MOVIE_ID = 27205L;

    @Mock private Ffmpeg ffmpeg;
    @Mock private PlaylistBuilder playlistBuilder;
    @Mock private StreamTokenService streamTokenService;
    @Mock private TorrentDownloadService torrentDownloadService;
    @Mock private MovieDownloadRepository movieDownloadRepository;
    @Mock private MagnetResolver magnetResolver;

    private StreamServiceImpl service;
    private Path movieFile;

    @BeforeEach
    void setUp(@TempDir Path storage) throws IOException {
        service = new StreamServiceImpl(
                ffmpeg, playlistBuilder, streamTokenService, new StreamProperties(),
                torrentDownloadService, movieDownloadRepository, magnetResolver);
        ReflectionTestUtils.setField(service, "videoStoragePath", storage.toString());

        movieFile = storage.resolve(String.valueOf(MOVIE_ID)).resolve("movie.mkv");
        Files.createDirectories(movieFile.getParent());
        Files.writeString(movieFile, "video");

        MovieDownload download = new MovieDownload();
        download.setMovieId(MOVIE_ID);
        download.setStatus(DownloadStatus.COMPLETED);
        download.setFileName("movie.mkv");
        when(movieDownloadRepository.findByMovieId(MOVIE_ID)).thenReturn(Optional.of(download));
        // No live progress entry means "fully on disk", so the movie is playable.
        when(torrentDownloadService.getProgress(MOVIE_ID)).thenReturn(null);
        when(streamTokenService.issue(any())).thenReturn("tok");
    }

    private void probeReturns(String audioLanguage, SubtitleTrack... tracks) {
        when(ffmpeg.probe(any())).thenReturn(
                new MediaInfo(movieFile, 1920, 1080, 600, audioLanguage, List.of(tracks)));
    }

    private User viewerPreferring(PreferredLanguage language) {
        User user = new User();
        user.setUsername("johndoe");
        user.setPreferredLanguage(language);
        return user;
    }

    private StreamSessionDto play(PreferredLanguage preference) {
        StreamSessionDto session = service.createSession(MOVIE_ID, viewerPreferring(preference));
        assertThat(session.getState()).isEqualTo(StreamState.READY);
        return session;
    }

    @Test
    void audioAlreadyInTheViewersLanguageLeavesSubtitlesOff() {
        probeReturns("eng",
                new SubtitleTrack(0, "eng", null, true),
                new SubtitleTrack(1, "fre", null, true));

        StreamSessionDto session = play(PreferredLanguage.ENGLISH);

        // Both stay selectable, but nothing is forced on.
        assertThat(session.getSubtitles()).hasSize(2);
        assertThat(session.getSubtitles()).noneMatch(SubtitleTrackDto::isDefaultTrack);
    }

    @Test
    void foreignAudioTurnsOnTheViewersOwnLanguage() {
        probeReturns("eng",
                new SubtitleTrack(0, "eng", null, true),
                new SubtitleTrack(1, "fre", null, true));

        StreamSessionDto session = play(PreferredLanguage.FRENCH);

        assertThat(session.getSubtitles())
                .filteredOn(SubtitleTrackDto::isDefaultTrack)
                .singleElement()
                .satisfies(track -> assertThat(track.getLanguage()).isEqualTo("fr"));
    }

    @Test
    void whenTheirLanguageIsMissingItFallsBackToEnglish() {
        probeReturns("fre",
                new SubtitleTrack(0, "eng", null, true),
                new SubtitleTrack(1, "spa", null, true));

        StreamSessionDto session = play(PreferredLanguage.ARABIC);

        assertThat(session.getSubtitles())
                .filteredOn(SubtitleTrackDto::isDefaultTrack)
                .singleElement()
                .satisfies(track -> assertThat(track.getLanguage()).isEqualTo("en"));
    }

    @Test
    void foreignAudioWithNoUsableSubtitlesSimplyEnablesNothing() {
        probeReturns("jpn", new SubtitleTrack(0, "spa", null, true));

        StreamSessionDto session = play(PreferredLanguage.ARABIC);

        assertThat(session.getSubtitles()).hasSize(1);
        assertThat(session.getSubtitles()).noneMatch(SubtitleTrackDto::isDefaultTrack);
    }

    @Test
    void theMenuLeadsWithTheViewersLanguageThenEnglish() {
        probeReturns("jpn",
                new SubtitleTrack(0, "spa", null, true),
                new SubtitleTrack(1, "eng", null, true),
                new SubtitleTrack(2, "fre", null, true));

        StreamSessionDto session = play(PreferredLanguage.FRENCH);

        assertThat(session.getSubtitles())
                .extracting(SubtitleTrackDto::getLanguage)
                .containsExactly("fr", "en", "es");
    }

    @Test
    void bitmapTracksAreNeverOfferedSinceTheyCannotBecomeWebVtt() {
        probeReturns("eng",
                new SubtitleTrack(0, "eng", null, true),
                new SubtitleTrack(1, "fre", "French PGS", false));

        StreamSessionDto session = play(PreferredLanguage.FRENCH);

        assertThat(session.getSubtitles())
                .extracting(SubtitleTrackDto::getLanguage)
                .containsExactly("en");
        // The only usable track is English, so that is what gets enabled.
        assertThat(session.getSubtitles().get(0).isDefaultTrack()).isTrue();
    }

    @Test
    void aViewerWithNoStatedPreferenceIsTreatedAsEnglish() {
        probeReturns("fre", new SubtitleTrack(0, "eng", null, true));

        StreamSessionDto session = play(null);

        assertThat(session.getSubtitles().get(0).isDefaultTrack()).isTrue();
    }

    @Test
    void trackTitlesAreUsedAsLabelsWhenThefileCarriesThem() {
        probeReturns("jpn",
                new SubtitleTrack(0, "eng", "English (SDH)", true),
                new SubtitleTrack(1, "eng", null, true));

        StreamSessionDto session = play(PreferredLanguage.ENGLISH);

        assertThat(session.getSubtitles())
                .extracting(SubtitleTrackDto::getLabel)
                .containsExactly("English (SDH)", "English");
        // Two tracks of the same language stay distinct because the URL is keyed on index.
        assertThat(session.getSubtitles())
                .extracting(SubtitleTrackDto::getUrl)
                .doesNotHaveDuplicates();
    }
}
