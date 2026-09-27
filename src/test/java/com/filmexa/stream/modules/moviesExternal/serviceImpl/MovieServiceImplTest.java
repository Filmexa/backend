/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieServiceImplTest.java                          :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/26 12:52:27 by marouan           #+#    #+#             */
/*   Updated: 2026/09/27 12:52:53 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.serviceImpl;

import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

import com.filmexa.stream.modules.moviesExternal.mapper.MovieMapper;
import com.filmexa.stream.modules.moviesExternal.client.MovieProvider;
import com.filmexa.stream.modules.moviesExternal.serviceImpl.MovieServiceImpl;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieDetailsProviderResponse;
import com.filmexa.stream.modules.moviesExternal.dto.response.TrendingMoviesResponse;

@ExtendWith(MockitoExtension.class)
public class MovieServiceImplTest {

    @Mock
    private MovieProvider movieProvider;

    @Mock
    private MovieMapper movieMapper;

    @InjectMocks 
    private MovieServiceImpl movieService;

    @Test
    void shouldReturnThenTrendingMovies() {
        // generate 20 movies returned from provider
        List<MovieDetailsProviderResponse> movies = new ArrayList<>();
        List<Integer> genre_ids = new ArrayList<>(List.of(27,878,12));
        for ( Long i = 0L; i < 20; i++ ) {
            movies.add( new MovieDetailsProviderResponse(
                i,
                "Resident Evil",
                "2026-09-16",
                "/pICoWjcKSet6sA3yzxOkP8ChwYI.jpg",
                "/3icyRAqgakNcQn6aDVz9libFmBA.jpg",
                "Bryan, un coursier médical, ...",
                7.318,
                genre_ids,
                false
            ));
        };
        when( movieProvider.getTrendingMovies("en"))
            .thenReturn( movies );
        
        List< TrendingMoviesResponse > trendingMovies = movieService.getTrendingMovies("en");
        assertEquals(10, trendingMovies.size() );
        assertThat( trendingMovies.get(0).getTitle() ).isEqualTo("Resident Evil");
    }
}
