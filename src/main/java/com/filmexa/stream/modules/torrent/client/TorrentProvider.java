/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TorrentProvider.java                               :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/20 12:37:37 by marouan           #+#    #+#             */
/*   Updated: 2026/09/21 10:13:46 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.torrent.client;

import com.filmexa.stream.modules.torrent.dto.TorrentProviderResponseData;

public interface TorrentProvider {
    TorrentProviderResponseData search( String imdbId );
}
