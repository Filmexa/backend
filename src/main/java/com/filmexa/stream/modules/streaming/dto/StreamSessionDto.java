package com.filmexa.stream.modules.streaming.dto;

import java.util.List;

import com.filmexa.stream.modules.download.enums.DownloadStatus;
import com.filmexa.stream.modules.streaming.enums.StreamState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The answer to "play this movie".
 *
 * <p>While {@code state} is PREPARING only the progress fields are filled in and the
 * client should poll again. Once it flips to READY the playback fields are populated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamSessionDto {

    private Long movieId;
    private StreamState state;

    private DownloadStatus downloadStatus;
    private double downloadProgressPercentage;
    private Long downloadedBytes;
    private Long totalBytes;
    private Double downloadSpeedBps;

    /** Set once READY: the HLS master playlist to hand to hls.js. */
    private String manifestUrl;
    private String token;
    private long expiresInSeconds;
    private double durationSeconds;

    /**
     * How much of the film is guaranteed playable right now, in seconds from the start:
     * draw it as the available range on the scrub bar. Seeking past it is allowed and
     * expected - the download jumps to wherever the viewer lands, so a 503 on a segment
     * means "buffering", not an error. A region fetched by an earlier seek can be
     * playable without being counted here, since this is the run that starts at 0.
     */
    private double playableSeconds;

    private List<VariantDto> variants;
    private List<SubtitleTrackDto> subtitles;
}
