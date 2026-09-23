package com.filmexa.stream.modules.download.magnet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.filmexa.stream.common.exception.NotFoundException;

class ConfiguredMagnetResolverTest {

    private static final Long MOVIE_ID = 27205L;
    private static final String CUSTOM = "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567";

    private MagnetProperties properties;
    private ConfiguredMagnetResolver resolver;

    @BeforeEach
    void setUp() {
        properties = new MagnetProperties();
        resolver = new ConfiguredMagnetResolver(properties);
    }

    @Test
    void theDefaultFallbackResolvesToAWorkingTestTorrent() {
        String magnet = resolver.resolve(MOVIE_ID);

        assertThat(magnet).startsWith("magnet:?xt=urn:btih:");
        assertThat(magnet).contains("dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c");
    }

    @Test
    void everyCatalogueEntryCarriesTrackers() {
        // bt-dht is not a dependency, so a magnet with no reachable tracker has no way
        // to find peers at all - there is no DHT fallback.
        assertThat(ConfiguredMagnetResolver.CATALOGUE.values()).allSatisfy(magnet ->
                assertThat(magnet).contains("tracker.opentrackr.org"));
    }

    @Test
    void aPerMovieOverrideBeatsTheFallback() {
        properties.getMovies().put(MOVIE_ID, CUSTOM);

        assertThat(resolver.resolve(MOVIE_ID)).isEqualTo(CUSTOM);
        assertThat(resolver.resolve(99999L)).contains("dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c");
    }

    @Test
    void aCatalogueNameWorksAsAnOverrideToo() {
        properties.getMovies().put(MOVIE_ID, "sintel");

        assertThat(resolver.resolve(MOVIE_ID))
                .contains("08ada5a7a6183aae1e09d831df6748d566095a10");
    }

    @Test
    void namesAreMatchedRegardlessOfCaseOrPadding() {
        properties.setFallback("  Big-Buck-Bunny  ");

        assertThat(resolver.resolve(MOVIE_ID)).contains("dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c");
    }

    @Test
    void aTypoFailsLoudlyInsteadOfDownloadingSomethingUnexpected() {
        properties.setFallback("big-buck-buny");

        assertThatThrownBy(() -> resolver.resolve(MOVIE_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("app.magnet.fallback")
                .hasMessageContaining("big-buck-buny");
    }
}
