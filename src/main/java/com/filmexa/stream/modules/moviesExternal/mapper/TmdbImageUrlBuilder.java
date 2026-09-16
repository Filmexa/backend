/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TmdbImageUrlBuilder.java                           :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/16 18:20:17 by kchaouki          #+#    #+#             */
/*   Updated: 2026/09/16 18:33:26 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.mapper;

public final class TmdbImageUrlBuilder {

    private TmdbImageUrlBuilder() { }

    public static String build(String baseUrl, String size, String path) {
        if (path == null) {
            return null;
        }

        String normalizedBase = baseUrl.endsWith("/")
            ? baseUrl.substring(0, baseUrl.length() - 1)
            : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;

        return normalizedBase + "/" + size + normalizedPath;
    }
}
