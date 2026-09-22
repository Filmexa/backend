package com.filmexa.stream.modules.download.service;


import com.filmexa.stream.modules.download.entity.MovieDownload;
import com.filmexa.stream.modules.download.dto.DownloadProgressDto;
import com.filmexa.stream.modules.download.dto.DownloadRequestDto;

public interface TorrentDownloadService {

    MovieDownload startDownload(DownloadRequestDto request);

    DownloadProgressDto getProgress(long movieId);

    void stopDownload(long movieId);

}
