package com.filmexa.stream.modules.streaming.controller;


import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.filmexa.stream.modules.streaming.dto.StreamSessionDto;
import com.filmexa.stream.modules.streaming.service.StreamService;
import com.filmexa.stream.modules.users.entity.User;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Playback endpoints.
 *
 * <p>Only {@code /session} authenticates with the usual bearer token - hls.js cannot set
 * headers on its playlist and segment requests, so those are unlocked by the
 * movie-scoped {@code ?token=} that {@code /session} hands out.
 */
@RestController
@RequestMapping("/api/stream/{movieId}")
@RequiredArgsConstructor
@Tag(name = "Streaming", description = "Movie playback and transcoding")
public class StreamController {

    private static final MediaType HLS_PLAYLIST = MediaType.parseMediaType("application/vnd.apple.mpegurl");
    private static final MediaType MPEG_TS = MediaType.parseMediaType("video/mp2t");
    private static final MediaType WEB_VTT = MediaType.parseMediaType("text/vtt");

    private final StreamService streamService;

    /** Starts playback: probes the movie if needed, returns the manifest URL and token. */
    @PostMapping("/session")
    public ResponseEntity<StreamSessionDto> createSession(@PathVariable Long movieId,
                                                          Authentication authentication) {
        return ResponseEntity.ok(
                streamService.createSession(movieId, (User) authentication.getPrincipal()));
    }

    @GetMapping("/master.m3u8")
    public ResponseEntity<String> masterPlaylist(@PathVariable Long movieId,
                                                 @RequestParam(required = false) String token) {
        return playlist(streamService.masterPlaylist(movieId, token));
    }

    @GetMapping("/{height:\\d+}/index.m3u8")
    public ResponseEntity<String> mediaPlaylist(@PathVariable Long movieId,
                                                @PathVariable int height,
                                                @RequestParam(required = false) String token) {
        return playlist(streamService.mediaPlaylist(movieId, height, token));
    }

    /**
     * One HLS segment, encoded on demand straight into the response.
     *
     * <p>The request is validated before any bytes are written, so "not downloaded yet"
     * still arrives as a 503 with Retry-After, which the player treats as buffering.
     */
    @GetMapping("/{height:\\d+}/seg-{index:\\d+}.ts")
    public ResponseEntity<byte[]> segment(@PathVariable Long movieId,
                                          @PathVariable int height,
                                          @PathVariable int index) {
        StreamService.Segment segment = streamService.prepareSegment(movieId, height, index);
        byte[] body = streamService.segmentBytes(segment);

        return ResponseEntity.ok()
                .contentType(MPEG_TS)
                .contentLength(body.length)
                // Nothing is cached server-side, so letting the browser keep segments is
                // what spares us re-encoding on a seek backwards.
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(6)).cachePrivate())
                .body(body);
    }

    /**
     * One subtitle track as WebVTT, for a {@code <track>} element on the video.
     */
    @GetMapping("/subtitles/{trackIndex:\\d+}.vtt")
    public ResponseEntity<Resource> subtitle(@PathVariable Long movieId,
                                             @PathVariable int trackIndex) {
        Path vtt = streamService.subtitle(movieId, trackIndex);

        return ResponseEntity.ok()
                .contentType(WEB_VTT)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(6)).cachePrivate())
                .body(new FileSystemResource(vtt));
    }

    private ResponseEntity<String> playlist(String content) {
        return ResponseEntity.ok()
                .contentType(HLS_PLAYLIST)
                .cacheControl(CacheControl.noStore())
                .body(content);
    }
}
