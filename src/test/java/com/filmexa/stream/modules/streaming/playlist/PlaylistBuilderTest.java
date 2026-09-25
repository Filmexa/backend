package com.filmexa.stream.modules.streaming.playlist;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.filmexa.stream.modules.streaming.config.StreamProperties;
import com.filmexa.stream.modules.streaming.dto.MediaInfo;
import com.filmexa.stream.modules.streaming.enums.Resolution;

class PlaylistBuilderTest {

    private PlaylistBuilder builder;
    private MediaInfo info;

    @BeforeEach
    void setUp() {
        StreamProperties properties = new StreamProperties();
        properties.setSegmentSeconds(6);
        builder = new PlaylistBuilder(properties);

        // 20 seconds: three full segments plus a 2 second tail.
        info = new MediaInfo(java.nio.file.Path.of("movie.mkv"), 1920, 1080, 20,
                java.util.List.of(new com.filmexa.stream.modules.streaming.dto.AudioTrack(0, "eng", null)),
                java.util.List.of());
    }

    @Test
    void mediaPlaylistCoversTheWholeMovieIncludingAShortFinalSegment() {
        String playlist = builder.media(info, null);

        assertThat(playlist).startsWith("#EXTM3U\n");
        assertThat(playlist).contains("#EXT-X-PLAYLIST-TYPE:VOD");
        assertThat(playlist).contains("#EXT-X-TARGETDURATION:6");
        assertThat(playlist).contains("seg-0.ts", "seg-1.ts", "seg-2.ts", "seg-3.ts");
        assertThat(playlist).doesNotContain("seg-4.ts");
        // The tail segment is 2s, not a padded 6s - otherwise the player's timeline
        // would run past the end of the movie.
        assertThat(playlist).contains("#EXTINF:2.000,");
        assertThat(playlist).endsWith("#EXT-X-ENDLIST\n");
    }

    @Test
    void segmentCountRoundsUpToCoverThePartialTail() {
        assertThat(builder.segmentCount(info)).isEqualTo(4);
    }

    @Test
    void masterPlaylistListsOneVariantPerRungWithTheSourceAspectRatio() {
        String playlist = builder.master(info, Resolution.ladderFor(1080), null);

        assertThat(playlist).contains("RESOLUTION=640x360");
        assertThat(playlist).contains("RESOLUTION=1280x720");
        assertThat(playlist).contains("RESOLUTION=1920x1080");
        assertThat(playlist).contains("360/index.m3u8", "1080/index.m3u8");
    }

    @Test
    void tokenIsCarriedIntoEveryUriBecauseRelativeUrisDropTheQueryString() {
        String master = builder.master(info, Resolution.ladderFor(720), "abc.def");
        String media = builder.media(info, "abc.def");

        assertThat(master).contains("720/index.m3u8?token=abc.def");
        assertThat(media).contains("seg-0.ts?token=abc.def");
    }
}
