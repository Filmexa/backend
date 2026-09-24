/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieDetailsResponse.java                          :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/17 23:31:37 by maddou            #+#    #+#             */
/*   Updated: 2026/09/23 01:03:43 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.dto.response;

import lombok.Data;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieDetailsResponse {
    Integer id;
    String  backdropPath;
    List< String > genres;
    String overview;
    String  releaseDate;
    String title;
    String imdbId;
    Double rating;
    String trailer;
    List< ActorResponse > actors;
}
