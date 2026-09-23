package com.filmexa.stream.modules.download.service;


import com.filmexa.stream.modules.download.entity.MovieDownload;
import com.filmexa.stream.modules.download.dto.DownloadProgressDto;
import com.filmexa.stream.modules.download.dto.DownloadRequestDto;

public interface TorrentDownloadService {

    MovieDownload startDownload(DownloadRequestDto request);

    DownloadProgressDto getProgress(Long movieId);

    void stopDownload(Long movieId);

    /** True when a torrent client is running for this movie in this process. */
    boolean isActive(Long movieId);

}
