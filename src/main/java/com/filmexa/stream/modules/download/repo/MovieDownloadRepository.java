package com.filmexa.stream.modules.download.repo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.filmexa.stream.modules.download.entity.MovieDownload;

public interface MovieDownloadRepository extends JpaRepository<MovieDownload, UUID> {

    Optional<MovieDownload> findByMovieId(Long movieId);

    List<MovieDownload> findByLastWatchedAtBefore(LocalDateTime cutoff);
}
