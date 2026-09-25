/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   YtsClient.java                                     :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/20 17:48:28 by marouan           #+#    #+#             */
/*   Updated: 2026/09/24 14:06:58 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.torrent.client;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;

// import com.filmexa.stream.modules.torrent.dto.TorrentProviderResponseData;
import com.filmexa.stream.modules.torrent.dto.yts.Movie;
import com.filmexa.stream.modules.torrent.dto.yts.YtsResponseDto;
import com.filmexa.stream.modules.torrent.dto.yts.YtsTorrentDto;
import com.filmexa.stream.modules.torrent.dto.TorrentResultDto;

import lombok.extern.slf4j.Slf4j;

@Component("ytsClient")
@Slf4j
public class YtsClient implements TorrentClient {

    private final RestClient restClient;
    
    public YtsClient( @Qualifier("YtsRestClient") RestClient restClient ) {
        this.restClient = restClient;    
    }

    @Override
    public List<TorrentResultDto> search( String imdbId ) {
        // fetch data from yts torrent
        YtsResponseDto response = restClient.get()
            .uri("?imdb_id={imdId}", imdbId )
            .retrieve()
            .body( YtsResponseDto.class );

        // YTS answers 200 with an empty payload for a film it does not carry: no data,
        // no movie, or a movie with no torrents. That is "nothing found", not an error,
        // so the other providers still get their turn.
        Movie movie = response == null || response.getData() == null
                ? null
                : response.getData().getMovie();
        List<YtsTorrentDto> dataYts = movie == null ? null : movie.getTorrents();

        if ( dataYts == null || dataYts.isEmpty() ) {
            log.info( "YTS has no torrent for {}", imdbId );
            return List.of();
        }

        Integer id = movie.getId();
        String releaseName = movie.getTitle_long() != null && !movie.getTitle_long().isBlank()
                ? movie.getTitle_long()
                : movie.getTitle();
        // map yts response to common client data
        return dataYts.stream()
            .map( torrent -> new TorrentResultDto(
                id,
                imdbId,
                torrent.getHash(),
                torrent.getSeeds(),
                torrent.getPeers(),
                torrent.getSize(),
                torrent.getSize_bytes(),
                torrent.getQuality(),
                torrent.getType(),
                torrent.getVideo_codec(),
                releaseName
            ))
            .toList();
    }
}