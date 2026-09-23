package com.filmexa.stream.modules.download.dto;

import com.filmexa.stream.modules.download.enums.DownloadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadProgressDto {

    private Long movieId;
    private DownloadStatus status;
    private double progressPercentage;
    private long downloadedBytes;
    private long totalBytes;
    private double downloadSpeedBps;
    private boolean isReadyToStream;
    private String containerFormat;
}
