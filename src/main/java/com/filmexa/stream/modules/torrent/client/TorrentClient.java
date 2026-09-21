/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TorrentClient.java                               :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/20 12:37:37 by marouan           #+#    #+#             */
/*   Updated: 2026/09/21 10:47:12 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.torrent.client;

import java.util.List;

import com.filmexa.stream.modules.torrent.dto.TorrentResultDto;
import com.filmexa.stream.modules.torrent.dto.yts.YtsData;
import com.filmexa.stream.modules.torrent.dto.yts.YtsTorrentDto;

public interface TorrentClient {
    List<TorrentResultDto> search( String imdbId );
}
