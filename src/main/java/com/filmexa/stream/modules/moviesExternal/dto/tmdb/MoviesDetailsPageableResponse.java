/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MoviesDetailsPageableResponse.java                 :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/19 23:43:54 by maddou            #+#    #+#             */
/*   Updated: 2026/09/19 23:47:04 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.dto.tmdb;

import java.util.List;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@AllArgsConstructor 
@NoArgsConstructor 
public class MoviesDetailsPageableResponse {
    // private int page;

    // @JsonProperty("total_pages")
    // private int totalPages;

    // @JsonProperty("total_results")
    // private int totalResults;

    private List<MovieDetailsProviderData> results;
}
