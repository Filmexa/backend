package com.filmexa.stream.modules.streaming.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LanguagesTest {

    @Test
    void threeLetterCodesBecomeTheTwoLetterFormATrackElementExpects() {
        assertThat(Languages.toBcp47("eng")).isEqualTo("en");
        assertThat(Languages.toBcp47("ara")).isEqualTo("ar");
    }

    @Test
    void bothIsoVariantsOfALanguageMapToTheSameCode() {
        // ffprobe reports either the bibliographic or the terminological code.
        assertThat(Languages.toBcp47("fre")).isEqualTo(Languages.toBcp47("fra"));
        assertThat(Languages.toBcp47("ger")).isEqualTo(Languages.toBcp47("deu"));
    }

    @Test
    void unknownCodesArePassedThroughRatherThanDropped() {
        assertThat(Languages.toBcp47("xyz")).isEqualTo("xyz");
        assertThat(Languages.displayName("xyz")).isEqualTo("XYZ");
    }

    @Test
    void untaggedTracksGetAReadableLabel() {
        assertThat(Languages.displayName("und")).isEqualTo("Unknown");
        assertThat(Languages.displayName(null)).isEqualTo("Unknown");
        assertThat(Languages.displayName("")).isEqualTo("Unknown");
    }

    @Test
    void codesAreMatchedRegardlessOfCaseOrPadding() {
        assertThat(Languages.toBcp47("  ENG ")).isEqualTo("en");
        assertThat(Languages.displayName("Eng")).isEqualTo("English");
    }
}
