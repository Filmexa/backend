/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieMapper.java                                   :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/16 12:06:56 by marouan           #+#    #+#             */
/*   Updated: 2026/09/16 18:32:26 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.mapper;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import com.filmexa.stream.modules.moviesExternal.dto.response.MovieResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MoviesProviderData;

@Component
public class MovieMapper {
    
    @Value("${tmdb.base-image-url}")
    private String imageBaseUrl;
    
    public MovieResponse toMovieResponse( MoviesProviderData movieProvider ) {
        return new MovieResponse(
            movieProvider.getId(),
            movieProvider.getTitle(),
            movieProvider.getRelease_date(),
            TmdbImageUrlBuilder.build( this.imageBaseUrl, "w500", movieProvider.getPoster_path() )
        );
    }
}
