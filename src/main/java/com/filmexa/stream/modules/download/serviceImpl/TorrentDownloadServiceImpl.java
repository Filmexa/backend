package com.filmexa.stream.modules.download.serviceImpl;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.filmexa.stream.modules.download.dto.DownloadProgressDto;
import com.filmexa.stream.modules.download.dto.DownloadRequestDto;
import com.filmexa.stream.modules.download.entity.MovieDownload;
import com.filmexa.stream.modules.download.enums.DownloadStatus;
import com.filmexa.stream.modules.download.repo.MovieDownloadRepository;
import com.filmexa.stream.modules.download.service.TorrentDownloadService;
import com.filmexa.stream.modules.download.worker.TorrentDownloadWorker;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TorrentDownloadServiceImpl implements TorrentDownloadService {

    private final MovieDownloadRepository movieDownloadRepository;
    private final TorrentDownloadWorker torrentDownloadWorker;

    public TorrentDownloadServiceImpl(
            MovieDownloadRepository movieDownloadRepository,
            TorrentDownloadWorker torrentDownloadWorker) {
        this.movieDownloadRepository = movieDownloadRepository; 
        this.torrentDownloadWorker = torrentDownloadWorker;
    }

    @Override
    public MovieDownload startDownload(DownloadRequestDto request) {
        Long movieId = request.getMovieId();
        String magnetUrl = request.getMagnetUrl();

        // 1. Check if this movie is already in our database
        Optional<MovieDownload> existing = movieDownloadRepository.findByMovieId(movieId);

        if (existing.isPresent()) {
            MovieDownload download = existing.get();
            DownloadStatus status = download.getStatus();

            // If already downloading or finished, return it immediately
            if (status == DownloadStatus.DOWNLOADING 
                || status == DownloadStatus.READY_TO_STREAM 
                || status == DownloadStatus.COMPLETED) {
                log.info("Movie {} is already in status: {}", movieId, status);
                return download;
            }

            // If it was PAUSED or FAILED, resume it!
            download.setStatus(DownloadStatus.PENDING);
            download.setLastWatchedAt(LocalDateTime.now());
            movieDownloadRepository.save(download);

            torrentDownloadWorker.startDownloadAsync(movieId, magnetUrl);
            return download;
        }

        // 2. It's a new download: create and save the entity
        MovieDownload newDownload = new MovieDownload();
        newDownload.setMovieId(movieId);
        newDownload.setMagnetUrl(magnetUrl);
        newDownload.setStatus(DownloadStatus.PENDING);
        newDownload.setLastWatchedAt(LocalDateTime.now());

        MovieDownload saved = movieDownloadRepository.save(newDownload);

        // 3. Launch background download!
        torrentDownloadWorker.startDownloadAsync(movieId, magnetUrl);

        return saved;
    }

    @Override
    public void stopDownload(Long movieId) {
        torrentDownloadWorker.stopDownload(movieId);
    }

    @Override
    public boolean isActive(Long movieId) {
        return torrentDownloadWorker.isActive(movieId);
    }

    @Override
    public DownloadProgressDto getProgress(Long movieId) {
        DownloadProgressDto activeProgress = torrentDownloadWorker.getProgress(movieId);
        if (activeProgress != null) {
            return activeProgress;
        }
        Optional<MovieDownload> existing = movieDownloadRepository.findByMovieId(movieId);
        if (existing.isEmpty()) {
            return null;
        }

        MovieDownload download = existing.get();
        // A finished download is 100% by definition - checking the byte counters first
        // reports 0% for any row written before those counters were persisted.
        double progress;
        if (download.getStatus() == DownloadStatus.COMPLETED) {
            progress = 100.0;
        } else if (download.getTotalBytes() > 0) {
            progress = ((double) download.getDownloadedBytes() / download.getTotalBytes()) * 100.0;
        } else {
            progress = 0.0;
        }

        return DownloadProgressDto.builder()
            .movieId(download.getMovieId())
            .status(download.getStatus())
            .progressPercentage(progress)
            .downloadedBytes(download.getDownloadedBytes())
            .totalBytes(download.getTotalBytes())
            .downloadSpeedBps(0.0) // Not actively downloading, so speed is 0
            .isReadyToStream(download.isReadyToStream())
            .containerFormat(download.getContainerFormat())
            .build();
    }
}

