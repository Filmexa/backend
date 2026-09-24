package com.filmexa.stream.modules.streaming.dto;

import java.nio.file.Path;
import java.util.List;

/**
 * What ffprobe tells us about a movie file. Just enough to build the playlists, the
 * resolution ladder and the subtitle menu - held in memory, never persisted.
 *
 * @param file            the video file on disk
 * @param width           source width in pixels
 * @param height          source height, which caps the resolution ladder
 * @param durationSeconds total runtime, used to lay out the segment list
 * @param audioTracks     every audio stream in the file, in -map order. A release often
 *                        ships the original language alongside dubs, so which one to play
 *                        is a choice rather than simply the first
 * @param subtitles       every subtitle stream in the file, convertible or not
 */
public record MediaInfo(Path file, int width, int height, double durationSeconds,
                        List<AudioTrack> audioTracks, List<SubtitleTrack> subtitles) {
}
