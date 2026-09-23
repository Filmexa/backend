package com.filmexa.stream.modules.streaming.util;

import java.util.Locale;
import java.util.Map;

/**
 * Maps the ISO 639-2 codes ffprobe reports ("eng", "fre") onto the two-letter BCP-47
 * codes a {@code <track srclang>} expects ("en", "fr"), plus a readable menu label.
 */
public final class Languages {

    private static final Map<String, String[]> KNOWN = Map.ofEntries(
            Map.entry("eng", new String[] { "en", "English" }),
            Map.entry("fre", new String[] { "fr", "French" }),
            Map.entry("fra", new String[] { "fr", "French" }),
            Map.entry("spa", new String[] { "es", "Spanish" }),
            Map.entry("ger", new String[] { "de", "German" }),
            Map.entry("deu", new String[] { "de", "German" }),
            Map.entry("ita", new String[] { "it", "Italian" }),
            Map.entry("por", new String[] { "pt", "Portuguese" }),
            Map.entry("dut", new String[] { "nl", "Dutch" }),
            Map.entry("nld", new String[] { "nl", "Dutch" }),
            Map.entry("ara", new String[] { "ar", "Arabic" }),
            Map.entry("rus", new String[] { "ru", "Russian" }),
            Map.entry("jpn", new String[] { "ja", "Japanese" }),
            Map.entry("kor", new String[] { "ko", "Korean" }),
            Map.entry("chi", new String[] { "zh", "Chinese" }),
            Map.entry("zho", new String[] { "zh", "Chinese" }),
            Map.entry("tur", new String[] { "tr", "Turkish" }),
            Map.entry("pol", new String[] { "pl", "Polish" }),
            Map.entry("swe", new String[] { "sv", "Swedish" }),
            Map.entry("hin", new String[] { "hi", "Hindi" }));

    private Languages() {
    }

    /** Two-letter code for {@code <track srclang>}, falling back to whatever we were given. */
    public static String toBcp47(String isoCode) {
        String key = normalise(isoCode);
        String[] entry = KNOWN.get(key);
        return entry != null ? entry[0] : key;
    }

    /** Human-readable name for the subtitle menu. */
    public static String displayName(String isoCode) {
        String key = normalise(isoCode);
        String[] entry = KNOWN.get(key);
        if (entry != null) {
            return entry[1];
        }
        return key.isEmpty() || "und".equals(key) ? "Unknown" : key.toUpperCase(Locale.ROOT);
    }

    private static String normalise(String isoCode) {
        return isoCode == null ? "" : isoCode.trim().toLowerCase(Locale.ROOT);
    }
}
