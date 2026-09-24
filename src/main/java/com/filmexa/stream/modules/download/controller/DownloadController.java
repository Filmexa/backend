package com.filmexa.stream.modules.download.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.filmexa.stream.modules.download.dto.DownloadProgressDto;
import com.filmexa.stream.modules.download.dto.DownloadRequestDto;
import com.filmexa.stream.modules.download.entity.MovieDownload;
import com.filmexa.stream.modules.download.service.TorrentDownloadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;



@RestController
@RequestMapping("/api/downloads")
@RequiredArgsConstructor
@Tag(name = "Downloads", description = "Torrent download management APIs")
public class DownloadController {

    private final TorrentDownloadService torrentDownloadService;

    @PostMapping
    public ResponseEntity<MovieDownload> startDownload(@Valid @RequestBody DownloadRequestDto request) {
        MovieDownload download = torrentDownloadService.startDownload(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(download);
    }

    @GetMapping("/{movieId}/progress")
    public ResponseEntity<DownloadProgressDto> getProgress(@PathVariable Long movieId) {
        DownloadProgressDto progress = torrentDownloadService.getProgress(movieId);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(progress);
    }

    @PostMapping("/{movieId}/stop")
    public ResponseEntity<Void> stopDownload(@PathVariable Long movieId) {
        torrentDownloadService.stopDownload(movieId);
        return ResponseEntity.ok().build();
    }
}
