/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   YtsClient.java                                     :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/20 17:48:28 by marouan           #+#    #+#             */
/*   Updated: 2026/09/25 22:03:31 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.torrent.client;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.filmexa.stream.modules.torrent.dto.yts.Movie;
import com.filmexa.stream.modules.torrent.dto.yts.YtsResponseDto;
import com.filmexa.stream.modules.torrent.dto.yts.YtsTorrentDto;
import com.filmexa.stream.modules.torrent.dto.TorrentResultDto;
import com.filmexa.stream.common.exception.ExternalServiceException;

import lombok.extern.slf4j.Slf4j;

@Component("ytsClient")
@Slf4j
public class YtsClient implements TorrentClient {

    private final RestClient restClient;
    
    public YtsClient( @Qualifier("YtsRestClient") RestClient restClient ) {
        this.restClient = restClient;    
    }

    @Override
    public List<TorrentResultDto> search(String imdbId) {
        try {
            // fetch data from yts torrent
            YtsResponseDto response = restClient.get()
                .uri("?imdb_id={imdbId}", imdbId)
                .retrieve()
                .body(YtsResponseDto.class);
            Movie movie = response == null || response.getData() == null
                    ? null
                    : response.getData().getMovie();
            List<YtsTorrentDto> dataYts = movie == null ? null : movie.getTorrents();

            // YTS returns an empty movie/torrent list for titles it does not carry; let
            // the torrent service try its other providers.
            if (dataYts == null || dataYts.isEmpty()) {
                log.info("YTS has no torrent for {}", imdbId);
                return List.of();
            }

            Integer id = movie.getId();
            String releaseName = movie.getTitle_long() != null && !movie.getTitle_long().isBlank()
                    ? movie.getTitle_long()
                    : movie.getTitle();
            return dataYts.stream()
                .map(torrent -> new TorrentResultDto(
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
        catch (RestClientException e) {
            throw new ExternalServiceException(
                "External service is unavailable",
                e
            );
        }
    }
}
