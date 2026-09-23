package com.filmexa.stream.modules.download.magnet;

import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.filmexa.stream.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Resolves magnets from configuration, so the play -> download -> transcode -> stream
 * pipeline can be exercised before a real torrent source exists.
 *
 * <p>Resolution order for a movie id:
 * <ol>
 *   <li>{@code app.magnet.movies.<id>} - a magnet URI, or a catalogue name below</li>
 *   <li>{@code app.magnet.fallback} - same, applied to every other movie</li>
 * </ol>
 *
 * <p>The catalogue holds Blender Foundation open movies hosted by the Internet Archive.
 * They are Creative Commons licensed, so distributing them is unambiguously legal, and
 * the Archive seeds them permanently, so they actually download. Every info hash here was
 * read out of the real {@code .torrent} rather than copied from a web page.
 *
 * <p>Note these torrents are seeded <em>only</em> by {@code bt1/bt2.archive.org}, so those
 * trackers have to be in the magnet - adding the usual public UDP trackers would find no
 * peers. They are HTTP trackers, which is why {@code bt-http-tracker-client} is a
 * dependency.
 *
 * <p>Replace this for production with a resolver that looks up the movie's IMDb id
 * ({@code MovieDetailsResponse.imdbId} already carries it), queries a torrent source by
 * it, and ranks candidates by seeders then quality. The subject wants two such sources.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConfiguredMagnetResolver implements MagnetResolver {

    /**
     * Well-seeded public trackers. These matter more than usual here: {@code bt-dht} is
     * not a dependency, so a magnet can only find peers through a tracker - there is no
     * DHT fallback to rescue a magnet whose trackers are dead.
     */
    private static final String TRACKERS =
            "&tr=" + encode("udp://tracker.opentrackr.org:1337/announce")
            + "&tr=" + encode("udp://open.demonii.com:1337/announce")
            + "&tr=" + encode("udp://exodus.desync.com:6969/announce")
            + "&tr=" + encode("udp://open.stealth.si:80/announce")
            + "&tr=" + encode("udp://tracker.torrent.eu.org:451/announce");

    /**
     * Known-good test torrents - the WebTorrent fixtures, all Blender Foundation open
     * movies under Creative Commons.
     *
     * <p>An earlier version of this class used the Internet Archive's own torrents.
     * Their info hashes were genuine, but they never resolved: the Archive serves those
     * torrents mainly through web seeds declared inside the {@code .torrent}, and a
     * magnet link carries no web seeds. With no DHT either, there was nothing left to
     * fetch metadata from. The first entry below is the one confirmed to download
     * end to end on this project - 276 MB of {@code Big Buck Bunny.mp4}.
     */
    static final Map<String, String> CATALOGUE = Map.of(
            // ~276 MB single mp4. Confirmed working; the best default for a quick test.
            "big-buck-bunny", magnet("dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c", "Big Buck Bunny"),
            // ~129 MB.
            "sintel", magnet("08ada5a7a6183aae1e09d831df6748d566095a10", "Sintel"),
            // ~571 MB.
            "tears-of-steel", magnet("209c8226b299b308beaf2b9cd3fb49212dbd13ec", "Tears of Steel"));

    private final MagnetProperties properties;

    @Override
    public String resolve(Long movieId) {
        String configured = properties.getMovies().get(movieId);
        if (configured != null) {
            return toMagnet(configured, movieId, "app.magnet.movies." + movieId);
        }
        return toMagnet(properties.getFallback(), movieId, "app.magnet.fallback");
    }

    private String toMagnet(String value, Long movieId, String source) {
        String trimmed = value == null ? "" : value.trim();

        if (trimmed.startsWith("magnet:")) {
            log.info("Movie {}: using the magnet from {}", movieId, source);
            return trimmed;
        }

        String known = CATALOGUE.get(trimmed.toLowerCase(Locale.ROOT));
        if (known != null) {
            log.warn("Movie {}: resolving to the '{}' test torrent from {} - "
                    + "this is placeholder wiring, not a real torrent lookup",
                    movieId, trimmed, source);
            return known;
        }

        throw new NotFoundException(source + " is '" + trimmed
                + "', which is neither a magnet URI nor one of " + CATALOGUE.keySet());
    }

    private static String magnet(String infoHash, String displayName) {
        return "magnet:?xt=urn:btih:" + infoHash + "&dn=" + encode(displayName) + TRACKERS;
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
