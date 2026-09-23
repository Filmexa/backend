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

    /** Set once READY: the HLS master playlist to hand to hls.js. */
    private String manifestUrl;
    private String token;
    private long expiresInSeconds;
    private double durationSeconds;
    private List<VariantDto> variants;
    private List<SubtitleTrackDto> subtitles;
}
