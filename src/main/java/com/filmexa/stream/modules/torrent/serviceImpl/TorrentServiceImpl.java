/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TorrentServiceImpl.java                            :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/23 13:22:25 by marouan           #+#    #+#             */
/*   Updated: 2026/09/23 21:44:15 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.torrent.serviceImpl;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;

import com.filmexa.stream.modules.torrent.client.TorrentClient;
import com.filmexa.stream.modules.torrent.service.TorrentService;
import com.filmexa.stream.modules.torrent.dto.TorrentResultDto;

@Service
public class TorrentServiceImpl implements TorrentService{

    private final TorrentClient ytsTorrentClient;
    private final TorrentClient pirateBayTorrentClient;

    public TorrentServiceImpl(  @Qualifier("ytsClient") TorrentClient yts,
        @Qualifier("pirateBay") TorrentClient pirateBay
    ) {
        this.ytsTorrentClient = yts;
        this.pirateBayTorrentClient = pirateBay;
    }

    @Override
    public Optional<TorrentResultDto> resolve( String imdbId ) {

        if ( imdbId.equals( null ) ) return null;
        // get piratebay torrent data 
        List<TorrentResultDto> torrentMovieData = new ArrayList<>(
            this.ytsTorrentClient.search(imdbId)
        );
        torrentMovieData.addAll(
            this.pirateBayTorrentClient.search(imdbId)
        );
        
        Optional<TorrentResultDto> selectedTorrent = this.select( torrentMovieData );
        selectedTorrent.ifPresent(torrent -> {
            torrent.setMagnet( "magnet:?xt=urn:btih:" + torrent.getMagnet());
        });
        return selectedTorrent;
    };

    private Optional<TorrentResultDto> select( List<TorrentResultDto> torrents) {
        
        return torrents.stream()
            .filter(torrent -> qualityRank(torrent.getQuality()) >= 0)
            .max(
                Comparator
                    .comparing((TorrentResultDto torrent) -> torrent.getSeeds() )
                    .thenComparing(
                        (TorrentResultDto torrent) ->
                            qualityRank(torrent.getQuality())
                    )
            );
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
