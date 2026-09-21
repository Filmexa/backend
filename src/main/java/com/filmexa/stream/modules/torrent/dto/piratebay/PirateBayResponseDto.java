/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   PirateBayResponseDto.java                          :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/20 15:35:32 by marouan           #+#    #+#             */
/*   Updated: 2026/09/21 16:06:30 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.torrent.dto.piratebay;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PirateBayResponseDto {
    private String  info_hash;
    private String  name;
    private Integer seeders;
    private Integer leechers;
    private String  size;
    private String  imdb;
}
// https://apibay.org/q.php?q=tt1375666

// id	"7349754"
// name	"Inception (2010) 1080p BrRip x264 - 1.85GB - YIFY"
// info_hash	"224BF45881252643DFC2E71ABC7B2660A21C68C4"
// leechers	"93"
// seeders	"845"
// size	"1991613584"
// num_files	"6"
// username	"YIFY"
// added	"1339547627"
// status	"vip"
// category	"207"
// imdb	"tt1375666"