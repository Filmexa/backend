/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TorrentServiceImpl.java                            :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/23 13:22:25 by marouan           #+#    #+#             */
/*   Updated: 2026/09/24 14:18:48 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.torrent.serviceImpl;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Pattern;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import com.filmexa.stream.modules.torrent.client.TorrentClient;
import com.filmexa.stream.modules.torrent.service.TorrentService;
import com.filmexa.stream.modules.torrent.dto.TorrentResultDto;

@Service
public class TorrentServiceImpl implements TorrentService{

    /**
     * The sources give us a bare info hash. A magnet built from the hash alone
     * leaves the client with no way to find peers except DHT, which is slow to
     * bootstrap and blocked on some networks, so we attach the open trackers
     * these torrents are announced on.
     */
    private static final List<String> DEFAULT_TRACKERS = List.of(
        "udp://tracker.opentrackr.org:1337/announce",
        "udp://open.demonii.com:1337/announce",
        "udp://open.stealth.si:80/announce",
        "udp://tracker.torrent.eu.org:451/announce",
        "udp://explodie.org:6969/announce",
        "udp://tracker.leechers-paradise.org:6969/announce",
        "udp://tracker.dler.org:6969/announce",
        "udp://tracker.bittor.pw:1337/announce",
        "udp://tracker-udp.gbitt.info:80/announce"
    );

    private final TorrentClient ytsTorrentClient;
    private final TorrentClient pirateBayTorrentClient;

    @Value("${app.torrent.trackers:}")
    private List<String> configuredTrackers = List.of();

    public TorrentServiceImpl(  @Qualifier("ytsClient") TorrentClient yts,
        @Qualifier("pirateBay") TorrentClient pirateBay
    ) {
        this.ytsTorrentClient = yts;
        this.pirateBayTorrentClient = pirateBay;
    }

    @Override
    public Optional<TorrentResultDto> resolve( String imdbId ) {

        // Was imdbId.equals(null), which is always false and throws on the very input it
        // meant to guard against. Callers unwrap the Optional, so returning null here
        // only moved the failure one frame up.
        if ( imdbId == null || imdbId.isBlank() ) return Optional.empty();
        // get piratebay torrent data 
        List<TorrentResultDto> torrentMovieData = new ArrayList<>(
            this.ytsTorrentClient.search(imdbId)
        );
        torrentMovieData.addAll(
            this.pirateBayTorrentClient.search(imdbId)
        );
        
        Optional<TorrentResultDto> selectedTorrent = this.select( torrentMovieData );
        selectedTorrent.ifPresent(torrent -> {
            torrent.setMagnet( buildMagnet( torrent.getMagnet(), torrent.getReleaseName() ));
        });
        return selectedTorrent;
    };

    private String buildMagnet( String infoHash, String displayName ) {

        StringBuilder magnet = new StringBuilder("magnet:?xt=urn:btih:").append(infoHash);

        String safeName = sanitizeDisplayName(displayName);
        if (!safeName.isBlank()) {
            magnet.append("&dn=").append(encode(safeName));
        }

        List<String> trackers = configuredTrackers.isEmpty() ? DEFAULT_TRACKERS : configuredTrackers;
        for (String tracker : trackers) {
            magnet.append("&tr=").append(encode(tracker));
        }

        return magnet.toString();
    }

    /**
     * Form encoding writes a space as '+', but the client reads the magnet back
     * through java.net.URI, which leaves '+' alone - so spaces have to go over
     * as %20 or they survive into the parsed name.
     */
    private String encode( String value ) {

        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * bt 1.10 parses a magnet off URI.getSchemeSpecificPart(), which is already
     * percent-decoded, and only then splits it on '&' and '='. A display name
     * containing either character therefore splits into a fragment the parser
     * cannot read, and it dies with an ArrayIndexOutOfBoundsException - encoding
     * the name correctly does not help. So we drop those two characters.
     */
    private String sanitizeDisplayName( String displayName ) {

        if (displayName == null) {
            return "";
        }
        return displayName.replace('&', ' ').replace('=', ' ').trim();
    }

    private Optional<TorrentResultDto> select( List<TorrentResultDto> torrents) {
        
        return torrents.stream()
            .filter(torrent -> qualityRank(torrent.getQuality()) >= 0)
            .max(
                Comparator
                    .comparing((TorrentResultDto torrent) -> !isDubbed( torrent.getReleaseName() ))
                    .thenComparing((TorrentResultDto torrent) -> torrent.getSeeds() )
                    .thenComparing(
                        (TorrentResultDto torrent) ->
                            qualityRank(torrent.getQuality())
                    )
            );
    }

    /**
     * Tags a release carries when its audio is a dub rather than the original.
     *
     * <p>MULTi and DUAL mean several audio tracks, VF/VFF/VFQ/TRUEFRENCH French ones,
     * DUBBED and Dublado say so outright. VOSTFR is deliberately absent: it means original
     * audio with French subtitles, which is exactly what we want.
     */
    private static final Pattern DUB_MARKERS = Pattern.compile(
        "\\b(multi|dual|dubbed|dublado|doublage|vf|vff|vfq|vfi|truefrench|hindi ?dub|dual ?audio)\\b",
        Pattern.CASE_INSENSITIVE);

    /**
     * Only a hint, never a filter: release names are free text, so a wrong guess must cost
     * the torrent its place in the ranking rather than remove it from the list entirely.
     */
    private boolean isDubbed( String releaseName ) {

        return releaseName != null && DUB_MARKERS.matcher(releaseName).find();
    }

    private int qualityRank(String quality) {
        if (quality == null) {
            return -1;
        }
        return switch (quality) {
            case "1080p" -> 3;
            case "720p"  -> 2;
            case "576p"  -> 1;
            case "480p"  -> 0;
            default      -> -1;
        };
    }
}
