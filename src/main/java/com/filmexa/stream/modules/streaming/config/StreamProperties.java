package com.filmexa.stream.modules.streaming.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Everything under {@code app.stream.*} in application.properties.
 */
@Component
@ConfigurationProperties(prefix = "app.stream")
@Data
public class StreamProperties {

    /** Length of a single HLS segment, in seconds. */
    private int segmentSeconds = 6;

    private String ffmpegPath = "ffmpeg";

    private String ffprobePath = "ffprobe";

    private String preset = "veryfast";

    private int maxConcurrentTranscodes = 4;

    private int transcodeTimeoutSeconds = 120;

    private int probeTimeoutSeconds = 30;

    private String tokenSecret;

    private long tokenTtlMinutes = 180;

    private int readaheadSeconds = 15;
}
