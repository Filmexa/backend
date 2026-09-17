/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TmdbMoviesPageableResponse.java                    :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/16 16:29:25 by marouan           #+#    #+#             */
/*   Updated: 2026/09/17 09:26:22 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.dto.tmdb;

import java.util.List;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MoviesProviderData;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TmdbMoviesPageableResponse {
    int page;
    int total_pages;
    int total_results;
    List<MoviesProviderData> results;
}
