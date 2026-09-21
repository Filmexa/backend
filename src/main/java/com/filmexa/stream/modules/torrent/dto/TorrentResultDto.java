/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TorrentResultDto.java                           :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/20 12:29:40 by marouan           #+#    #+#             */
/*   Updated: 2026/09/20 15:39:52 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.torrent.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TorrentResultDto {
    private String  imdb;
    private String  hash;
    private Integer seeds;
    private Integer peers;
    private String  size;
    private Long  sizeBytes;
    private String  quality;
}
// https://movies-api.accel.li/api/v2/movie_details.json?imdb_id=tt1375666
// // yts
// {
//     "url": "https://yts.gg/torrent/download/CDED33F7FBF3E4E073778848FAD17674C0A35B82",
//     "hash": "CDED33F7FBF3E4E073778848FAD17674C0A35B82",
//     "quality": "720p",
//     "is_repack": "0",
//     "video_codec": "x264",
//     "bit_depth": "8",
//     "audio_channels": "2.0",
//     "seeds": 13,
//     "peers": 2,
//     "size": "809.06 MB",
//     "size_bytes": 848360899,
//     "date_uploaded": "2015-10-31 22:22:51",
//     "date_uploaded_unix": 1446326571
// },

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
