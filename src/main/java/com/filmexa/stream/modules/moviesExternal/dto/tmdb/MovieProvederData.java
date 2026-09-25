/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieProvederData.java                             :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: baani <baani@student.42.fr>                +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/17 23:07:04 by marouan           #+#    #+#             */
/*   Updated: 2026/09/25 15:52:31 by baani            ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.dto.tmdb;


import lombok.Data;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieProvederData {
    Double      vote_average;    
    Integer     id;
    String      title;
    String      backdrop_path; 
    String      poster_path;
    String      imdb_id;
    /** ISO 639-1 code of the language the film was shot in, e.g. "en", "ko". */
    String      original_language;
    List< GenreProviderData > genres;
    String      overview; 
    String      release_date;
    // Double      vote_average;
    boolean     adult;
    MovieCreditsProviderData    credits;
}
