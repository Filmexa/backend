package com.filmexa.stream.modules.download.service;


import com.filmexa.stream.modules.download.entity.MovieDownload;
import com.filmexa.stream.modules.download.dto.DownloadProgressDto;
import com.filmexa.stream.modules.download.dto.DownloadRequestDto;

import java.util.Optional;

public interface TorrentDownloadService {

    MovieDownload startDownload(DownloadRequestDto request);

    DownloadProgressDto getProgress(Long movieId);

    void stopDownload(Long movieId);

    /**
     * Tells a running download that playback moved, so it fetches that part of the film
     * first. Does nothing when no download is running for the movie.
     *
     * @param fraction how far into the file, 0.0 to 1.0
     */
    void seek(Long movieId, double fraction);

    /**
     * Whether the pieces covering a stretch of the file are on disk, as a fraction of it.
     *
     * @return the answer, or empty when no running download can give one
     */
    Optional<Boolean> isRangeDownloaded(Long movieId, double fromFraction, double toFraction);

    /** True when a torrent client is running for this movie in this process. */
    boolean isActive(Long movieId);

}
