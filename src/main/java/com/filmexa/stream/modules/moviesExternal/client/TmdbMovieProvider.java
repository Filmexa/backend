/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TmdbMovieProvider.java                             :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/13 10:29:13 by maddou            #+#    #+#             */
/*   Updated: 2026/09/14 19:15:08 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.client;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;
import org.springframework.core.ParameterizedTypeReference;

import com.filmexa.stream.modules.moviesExternal.client.MovieProvider;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.TmdbTrendingMoviesResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.TrendingMovieProviderResponse;

import java.util.List;

@Component
public class TmdbMovieProvider implements MovieProvider {

    private final RestClient restClient;

    public TmdbMovieProvider( @Qualifier("tmdbRestClient")  RestClient restClient ) {
        this.restClient = restClient;
    }

    @Override
    public List< TrendingMovieProviderResponse > getTrendingMovies( String language ){
        TmdbTrendingMoviesResponse response =  this.restClient
            .get()
            .uri("/trending/movie/week?language={language}", language)
            .retrieve()
            .body( TmdbTrendingMoviesResponse.class );
        return response.getResults();
    }
}
