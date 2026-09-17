/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieGenreMapper.java                              :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 20:52:30 by maddou            #+#    #+#             */
/*   Updated: 2026/09/17 12:25:24 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.mapper;

import java.util.Map;

public class MovieGenreMapper {

    private static final Map<Integer, Map<String, String>> GENRES = Map.ofEntries(

        Map.entry(28, Map.of(
            "en", "Action",
            "fr", "Action",
            "ar", "أكشن"
        )),

        Map.entry(12, Map.of(
            "en", "Adventure",
            "fr", "Aventure",
            "ar", "مغامرة"
        )),

        Map.entry(16, Map.of(
            "en", "Animation",
            "fr", "Animation",
            "ar", "رسوم متحركة"
        )),

        Map.entry(35, Map.of(
            "en", "Comedy",
            "fr", "Comédie",
            "ar", "كوميديا"
        )),

        Map.entry(80, Map.of(
            "en", "Crime",
            "fr", "Crime",
            "ar", "جريمة"
        )),

        Map.entry(99, Map.of(
            "en", "Documentary",
            "fr", "Documentaire",
            "ar", "وثائقي"
        )),

        Map.entry(18, Map.of(
            "en", "Drama",
            "fr", "Drame",
            "ar", "دراما"
        )),

        Map.entry(10751, Map.of(
            "en", "Family",
            "fr", "Familial",
            "ar", "عائلي"
        )),

        Map.entry(14, Map.of(
            "en", "Fantasy",
            "fr", "Fantastique",
            "ar", "فانتازيا"
        )),

        Map.entry(36, Map.of(
            "en", "History",
            "fr", "Histoire",
            "ar", "تاريخي"
        )),

        Map.entry(27, Map.of(
            "en", "Horror",
            "fr", "Horreur",
            "ar", "رعب"
        )),

        Map.entry(10402, Map.of(
            "en", "Music",
            "fr", "Musique",
            "ar", "موسيقى"
        )),

        Map.entry(9648, Map.of(
            "en", "Mystery",
            "fr", "Mystère",
            "ar", "غموض"
        )),

        Map.entry(10749, Map.of(
            "en", "Romance",
            "fr", "Romance",
            "ar", "رومانسي"
        )),

        Map.entry(878, Map.of(
            "en", "Science Fiction",
            "fr", "Science-fiction",
            "ar", "خيال علمي"
        )),

        Map.entry(10770, Map.of(
            "en", "TV Movie",
            "fr", "Téléfilm",
            "ar", "فيلم تلفزيوني"
        )),

        Map.entry(53, Map.of(
            "en", "Thriller",
            "fr", "Thriller",
            "ar", "إثارة"
        )),

        Map.entry(10752, Map.of(
            "en", "War",
            "fr", "Guerre",
            "ar", "حرب"
        )),

        Map.entry(37, Map.of(
            "en", "Western",
            "fr", "Western",
            "ar", "غربي"
        ))
    );

    public static String getName( Integer id, String language ) {
        if ( id == null || language == null) {
            return null;
        }
        Map<String, String> translations = GENRES.get(id);

        if (translations == null) {
            return null;
        }

        return translations.get(language);
    }
}