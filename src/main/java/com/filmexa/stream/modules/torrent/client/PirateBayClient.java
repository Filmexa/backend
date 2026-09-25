/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   PirateBayClient.java                                :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/21 15:15:36 by marouan           #+#    #+#             */
/*   Updated: 2026/09/21 15:47:10 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.torrent.client;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.HtmlUtils;

import com.filmexa.stream.modules.torrent.dto.TorrentResultDto;
import com.filmexa.stream.modules.torrent.dto.piratebay.PirateBayResponseDto;
import com.filmexa.stream.modules.torrent.parser.TorrentNameParser;
import com.filmexa.stream.common.exception.ExternalServiceException;

@Component("pirateBay")
public class PirateBayClient implements TorrentClient{

    private final RestClient restClient;
    private final TorrentNameParser parser;

    public PirateBayClient( @Qualifier("PirateBayRestClient") RestClient restClient,
        TorrentNameParser parser 
    ) {
        this.restClient = restClient;
        this.parser = parser;
    }

    public  List<TorrentResultDto> search( String imdbId ) {
        try {
            List< PirateBayResponseDto >  response = this.restClient.get()
                .uri("?q={imdbId}", imdbId )
                .retrieve()
                .body( new ParameterizedTypeReference<List<PirateBayResponseDto>>() {} );
            if ( response == null ) {
                return List.of();
            }
            return response.stream()
                .takeWhile(torrent -> torrent.getSeeders() > 1 )
                .map( torrent -> new TorrentResultDto(
                    torrent.getId(),
                    imdbId,
                    torrent.getInfo_hash(),
                    torrent.getSeeders(),
                    torrent.getLeechers(),
                    this.formatSize( Long.parseLong( torrent.getSize() ) ),
                    Long.parseLong( torrent.getSize() ),
                    this.parser.extractQuality(torrent.getName()),
                    this.parser.extractSource(torrent.getName()),
                    this.parser.extractVideoCodec(torrent.getName()),
                    torrent.getName()
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

    private String formatSize(Long bytes) {
        double gb = bytes / 1_000_000_000.0;
    
        if (gb >= 1) {
            return String.format("%.2f GB", gb);
        }
    
        double mb = bytes / 1_000_000.0;
        return String.format("%.2f MB", mb);
    }
}
