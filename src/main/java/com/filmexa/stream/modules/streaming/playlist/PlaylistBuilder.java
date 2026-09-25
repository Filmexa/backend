package com.filmexa.stream.modules.streaming.playlist;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.filmexa.stream.modules.streaming.config.StreamProperties;
import com.filmexa.stream.modules.streaming.dto.MediaInfo;
import com.filmexa.stream.modules.streaming.enums.Resolution;

import lombok.RequiredArgsConstructor;

/**
 * Generates the HLS playlists ourselves rather than letting ffmpeg write them.
 *
 * <p>We know the runtime from the probe and we know the segment length, so the full
 * segment list can be emitted before a single frame has been encoded. That is what
 * makes seeking work on a movie that is still downloading: the player asks for the
 * segment it wants, and we encode it then.
 */
@Component
@RequiredArgsConstructor
public class PlaylistBuilder {

    private final StreamProperties properties;

    public int segmentCount(MediaInfo info) {
        return (int) Math.ceil(info.durationSeconds() / properties.getSegmentSeconds());
    }

    /**
     * Master playlist: one entry per rung of the ladder.
     *
     * <p>The token is appended to every variant URI because a player resolves the
     * relative URI against the playlist's path, which drops the query string.
     */
    public String master(MediaInfo info, List<Resolution> ladder, String token) {
        StringBuilder playlist = new StringBuilder();
        playlist.append("#EXTM3U\n");
        playlist.append("#EXT-X-VERSION:3\n");

        for (Resolution resolution : ladder) {
            int width = scaledWidth(info, resolution);
            playlist.append("#EXT-X-STREAM-INF:BANDWIDTH=")
                    .append(resolution.getTotalBitrateBps())
                    .append(",RESOLUTION=").append(width).append('x').append(resolution.getHeight())
                    .append(",CODECS=\"avc1.640029,mp4a.40.2\"")
                    .append(",NAME=\"").append(resolution.getLabel()).append("\"\n");
            playlist.append(resolution.getHeight()).append("/index.m3u8")
                    .append(query(token)).append('\n');
        }

        return playlist.toString();
    }

    /**
     * Media playlist for a single rung. Marked as VOD with a full segment list and an
     * end tag, so the player allows seeking across the whole movie.
     */
    public String media(MediaInfo info, String token) {
        int segmentSeconds = properties.getSegmentSeconds();
        int count = segmentCount(info);
        double duration = info.durationSeconds();

        StringBuilder playlist = new StringBuilder();
        playlist.append("#EXTM3U\n");
        playlist.append("#EXT-X-VERSION:3\n");
        playlist.append("#EXT-X-PLAYLIST-TYPE:VOD\n");
        playlist.append("#EXT-X-TARGETDURATION:").append(segmentSeconds).append('\n');
        playlist.append("#EXT-X-MEDIA-SEQUENCE:0\n");

        for (int index = 0; index < count; index++) {
            double segmentDuration = Math.min(segmentSeconds, duration - (double) index * segmentSeconds);
            if (segmentDuration <= 0) {
                break;
            }
            playlist.append(String.format(Locale.ROOT, "#EXTINF:%.3f,", segmentDuration)).append('\n');
            playlist.append("seg-").append(index).append(".ts").append(query(token)).append('\n');
        }

        playlist.append("#EXT-X-ENDLIST\n");
        return playlist.toString();
    }

    /** Keeps the source aspect ratio, rounded to an even width as h264 requires. */
    public int scaledWidth(MediaInfo info, Resolution resolution) {
        if (info.height() <= 0 || info.width() <= 0) {
            return resolution.getHeight() * 16 / 9;
        }
        int width = (int) Math.round((double) info.width() * resolution.getHeight() / info.height());
        return width % 2 == 0 ? width : width + 1;
    }

    private String query(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        return "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
