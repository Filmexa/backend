/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TorrentNameParser.java                             :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/21 16:16:54 by marouan           #+#    #+#             */
/*   Updated: 2026/09/24 14:10:15 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.torrent.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class TorrentNameParser {

    private static final Pattern QUALITY_PATTERN =
        Pattern.compile("(?i)\\b(2160p|1440p|1080p|720p|576p|480p|360p)\\b");

    private static final Pattern SOURCE_PATTERN =
            Pattern.compile("(?i)\\b(BluRay|BRRip|BDRip|WEB-DL|WEBRip|HDRip|DVDRip|HDTV|CAM|TS)\\b");
    
    private static final Pattern CODEC_PATTERN =
            Pattern.compile("(?i)\\b(x264|x265|H\\.264|H\\.265|AV1|VP9)\\b");
    public String extractQuality(String name) {
        return extract(name, QUALITY_PATTERN);
    }

    public String extractSource(String name) {
        return extract(name, SOURCE_PATTERN);
    }

    public String extractVideoCodec(String name) {
        return extract(name, CODEC_PATTERN);
    }
    private String extract(String name, Pattern pattern) {
        Matcher matcher = pattern.matcher(name);

        return matcher.find()
                ? matcher.group(1)
                : null;
    }
}
