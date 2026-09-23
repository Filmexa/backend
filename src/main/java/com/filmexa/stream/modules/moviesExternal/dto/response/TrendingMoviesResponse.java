/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TrendingMoviesResponse.java                        :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 20:07:50 by maddou            #+#    #+#             */
/*   Updated: 2026/09/22 15:13:47 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.dto.response;

import java.util.List;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrendingMoviesResponse {
    Long        id;
    String      title;
    String      releaseDate;
    String      thumbnail;
    String      backdropUrl;
    String      overview;
    Double      rating;
    String      trailer;
    List<String> genres;
}
