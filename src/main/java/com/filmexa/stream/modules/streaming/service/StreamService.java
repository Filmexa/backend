package com.filmexa.stream.modules.streaming.service;


import com.filmexa.stream.modules.users.entity.User;
import com.filmexa.stream.modules.streaming.dto.MediaInfo;
import com.filmexa.stream.modules.streaming.dto.StreamSessionDto;
import com.filmexa.stream.modules.streaming.enums.Resolution;

public interface StreamService {

    /** Issues a stream token and describes how the client should play this movie. */
    StreamSessionDto createSession(Long movieId, User viewer);

    String masterPlaylist(Long movieId, String token);

    String mediaPlaylist(Long movieId, int height, String token);

    Segment prepareSegment(Long movieId, int height, int segmentIndex);

    /** Encodes the prepared segment and returns the MPEG-TS bytes. */
    byte[] segmentBytes(Segment segment);

    /**
     * The WebVTT file for one subtitle track, extracted on first request and kept.
     *
     * @throws com.filmexa.stream.common.exception.NotFoundException if the track does not
     *         exist or is a bitmap format that cannot become WebVTT
     */
    java.nio.file.Path subtitle(Long movieId, int trackIndex);

    record Segment(MediaInfo info, Resolution resolution, int index) {
    }
}
