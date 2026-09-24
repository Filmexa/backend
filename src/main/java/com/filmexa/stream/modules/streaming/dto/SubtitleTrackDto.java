package com.filmexa.stream.modules.streaming.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A WebVTT track the player can attach as a {@code <track>} element. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleTrackDto {

    /** BCP-47 code for the {@code srclang} attribute, e.g. "en". */
    private String language;

    /** What to show in the subtitle menu, e.g. "English" or "English (SDH)". */
    private String label;

    private String url;

    /**
     * True for the track the player should enable by itself: set when the movie's audio is
     * not in the viewer's preferred language. Exactly one track at most is marked.
     */
    private boolean defaultTrack;
}
