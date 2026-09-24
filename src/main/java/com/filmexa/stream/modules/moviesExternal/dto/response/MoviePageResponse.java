/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MoviePageResponse.java                             :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/17 09:29:38 by marouan           #+#    #+#             */
/*   Updated: 2026/09/17 09:39:41 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.dto.response;

import java.util.List;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import com.filmexa.stream.modules.moviesExternal.dto.response.MovieResponse;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MoviePageResponse {
    int page;
    int totalPages;
    int totalResults;
    List<MovieResponse> movies;
}
