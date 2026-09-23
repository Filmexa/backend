package com.filmexa.stream.modules.streaming.dto;

/**
 * One subtitle stream found inside the movie file.
 *
 * @param index       position among the file's subtitle streams - the "s:N" in ffmpeg's -map,
 *                    and what the URL is keyed on, since a file can carry two tracks of the
 *                    same language (a plain one and an SDH one, say)
 * @param language    ISO code as ffprobe reports it ("eng", "fre"), or "und" when untagged
 * @param title       the track's own name, when the file bothers to carry one
 * @param convertible false for bitmap formats (PGS, VobSub, DVB), which hold pictures of
 *                    text rather than text and so have no WebVTT equivalent
 */
public record SubtitleTrack(int index, String language, String title, boolean convertible) {
}
