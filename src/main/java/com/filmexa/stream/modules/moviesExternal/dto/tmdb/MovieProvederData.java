/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieProvederData.java                             :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/17 23:07:04 by marouan           #+#    #+#             */
/*   Updated: 2026/09/17 23:24:50 by maddou           ###   ########.fr       */
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
    Integer     id;
    String      title;
    String      backdrop_path; 
    String      imdb_id;
    List< GenreProviderData > genres;
    String      overview; 
    String      release_date;
    MovieCreditsProviderData    Credits;
}
