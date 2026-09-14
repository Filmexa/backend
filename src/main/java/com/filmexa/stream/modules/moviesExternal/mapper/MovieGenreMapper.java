/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieGenreMapper.java                              :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 20:52:30 by maddou            #+#    #+#             */
/*   Updated: 2026/09/14 20:52:52 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package src.main.java.com.filmexa.stream.modules.moviesExternal.mapper;

import java.util.Map;

public class MovieGenreMapper {

    private static final Map<Integer, String> GENRES = Map.of(
        28, "Action",
        18, "Drama",
        35, "Comedy",
        27, "Horror",
        10749, "Romance",
        878, "Science Fiction"
    );

    public static String getName(int id) {
        return GENRES.get(id);
    }
}