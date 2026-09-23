package com.filmexa.stream.modules.streaming.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ResolutionTest {

    @Test
    void ladderNeverOffersARungAboveTheSource() {
        List<Resolution> ladder = Resolution.ladderFor(720);

        assertThat(ladder).containsExactly(
                Resolution.P144, Resolution.P240, Resolution.P360, Resolution.P480, Resolution.P720);
    }

    @Test
    void ladderForA1080pSourceOffersEveryRung() {
        assertThat(Resolution.ladderFor(1080)).containsExactly(Resolution.values());
    }

    @Test
    void ladderForALowSourceOffersOnlyTheRungsBelowIt() {
        assertThat(Resolution.ladderFor(240)).containsExactly(Resolution.P144, Resolution.P240);
    }

    @Test
    void sourceSmallerThanTheLowestRungStillGetsOneVariant() {
        assertThat(Resolution.ladderFor(100)).containsExactly(Resolution.P144);
    }

    @Test
    void byHeightFindsAKnownRung() {
        assertThat(Resolution.byHeight(480)).contains(Resolution.P480);
        assertThat(Resolution.byHeight(1234)).isEmpty();
    }
}
