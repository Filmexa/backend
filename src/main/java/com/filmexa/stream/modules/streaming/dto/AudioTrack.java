package com.filmexa.stream.modules.streaming.dto;

/**
 * One audio stream inside the movie file.
 *
 * @param index    position among the file's audio streams - the "a:N" in ffmpeg's -map.
 *                 A release often carries several: the original plus one dub per market.
 * @param language ISO code as ffprobe reports it ("eng", "fre"), or "und" when untagged
 * @param title    the track's own name, when the file bothers to carry one
 */
public record AudioTrack(int index, String language, String title) {
}
