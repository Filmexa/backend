package com.filmexa.stream.modules.download.magnet;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Test-time wiring for {@link MagnetResolver}, under {@code app.magnet.*}.
 */
@Component
@ConfigurationProperties(prefix = "app.magnet")
@Data
public class MagnetProperties {

    /**
     * Used for any movie without its own entry below. Either a catalogue name
     * ("big-buck-bunny") or a full {@code magnet:?...} URI.
     */
    private String fallback = "big-buck-bunny";

    /**
     * Per-movie overrides, keyed by TMDB id:
     * {@code app.magnet.movies.27205=magnet:?xt=urn:btih:...}
     * Values may also be catalogue names.
     */
    private Map<Long, String> movies = new HashMap<>();
}
