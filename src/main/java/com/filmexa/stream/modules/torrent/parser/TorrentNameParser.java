/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TorrentNameParser.java                             :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/21 16:16:54 by marouan           #+#    #+#             */
/*   Updated: 2026/09/21 16:23:45 by marouan          ###   ########.fr       */
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

    public String extractQuality(String name) {
        Matcher matcher = QUALITY_PATTERN.matcher(name);

        return matcher.find()
            ? matcher.group(1)
            : null;
    }
}
