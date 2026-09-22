/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieDetailsResponse.java                          :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/17 23:31:37 by maddou            #+#    #+#             */
/*   Updated: 2026/09/22 14:11:53 by marouan          ###   ########.fr       */
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
    List< ActorResponse > actors;
}
