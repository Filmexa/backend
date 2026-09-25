/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   YtsClient.java                                     :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/20 17:48:28 by marouan           #+#    #+#             */
/*   Updated: 2026/09/25 20:56:12 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.torrent.client;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// import com.filmexa.stream.modules.torrent.dto.TorrentProviderResponseData;
import com.filmexa.stream.modules.torrent.dto.yts.YtsResponseDto;
import com.filmexa.stream.modules.torrent.dto.yts.YtsTorrentDto;
import com.filmexa.stream.modules.torrent.dto.TorrentResultDto;
import com.filmexa.stream.common.exception.ExternalServiceException;

@Component("ytsClient")
public class YtsClient implements TorrentClient {

    private final RestClient restClient;
    
    public YtsClient( @Qualifier("YtsRestClient") RestClient restClient ) {
        this.restClient = restClient;    
    }

    @Override
    public List<TorrentResultDto> search( String imdbId ) {
        try {
            // fetch data from yts torrent
            YtsResponseDto response = restClient.get()
                .uri("?imdb_id={imdId}", imdbId )
                .retrieve()
                .body( YtsResponseDto.class );
            if (response == null
                || response.getData() == null
                || response.getData().getMovie() == null) {
                    throw new ExternalServiceException(
                        "External service is unavailable"
                    );
            }
            Integer id = response.getData().getMovie().getId();
            List<YtsTorrentDto> dataYts =  response.getData().getMovie().getTorrents();
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
                    null
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