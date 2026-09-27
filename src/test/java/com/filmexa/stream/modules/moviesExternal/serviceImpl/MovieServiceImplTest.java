/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieServiceImplTest.java                          :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/26 12:52:27 by marouan           #+#    #+#             */
/*   Updated: 2026/09/27 21:35:53 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.serviceImpl;

import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import  org.mockito.Spy;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.filmexa.stream.modules.moviesExternal.mapper.MovieMapper;
import com.filmexa.stream.modules.moviesExternal.client.MovieProvider;
import com.filmexa.stream.modules.moviesExternal.serviceImpl.MovieServiceImpl;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieDetailsProviderResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MoviesProviderData;
import com.filmexa.stream.modules.moviesExternal.dto.response.TrendingMoviesResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.TmdbMoviesPageableResponse;
import com.filmexa.stream.modules.moviesExternal.dto.response.MoviePageResponse;
import com.filmexa.stream.common.exception.InvalidPaginationException;
import com.filmexa.stream.common.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class MovieServiceImplTest {

    @Mock
    private MovieProvider movieProvider;

    @Spy
    private MovieMapper movieMapper;

    @InjectMocks 
    private MovieServiceImpl movieService;

    @Value("${tmdb.base-image-url}")
    private String imageBaseUrl;
    
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
        
        // when( movieMapper.toMovieResponse( movies.get(0)) ).
        List< TrendingMoviesResponse > trendingMovies = movieService.getTrendingMovies("en");
        assertEquals(10, trendingMovies.size() );
        assertThat( trendingMovies.get(0).getTitle() ).isEqualTo("Resident Evil");
    }

    @Test 
    void shouldReturnMoviesByGenre( ) {
        Pageable pageable = PageRequest.of(0, 20);

        List<MoviesProviderData> movies = new ArrayList<>();
        for ( Long i = 0L; i < 20; i++ ) {
            movies.add( new MoviesProviderData(
                i,
                "Spider-Man: Brand New Day",
                "2026-07-29",
                7.864,
                "/bjiS5ipwxb9JFy3XRRN4OAilSeX.jpg",
                false
            ));
        };
        
        TmdbMoviesPageableResponse prviderResult = new TmdbMoviesPageableResponse(
            1,
            500,
            10000,
            movies
        );
        
        when( movieProvider.getMoviesByGenre("en", 28, 1) )
            .thenReturn( prviderResult );
        
        MoviePageResponse response = movieService.getMoviesByGenre( "en", 28, pageable );
        assertThat(response).isNotNull();
        assertThat( response.getMovies() )
            .hasSize(20);
        assertThat( response.getMovies().get(0).getTitle() )
            .isEqualTo("Spider-Man: Brand New Day");
        assertThat( response.getMovies().get(0).getThumbnail() )
            .isEqualTo( imageBaseUrl + "/bjiS5ipwxb9JFy3XRRN4OAilSeX.jpg" );
        
        verify( movieProvider ).getMoviesByGenre("en", 28, 1 );
    }

    @Test 
    void shouldThrownInvalidPaginationException() {
        Pageable pageable = PageRequest.of(501, 20);

        InvalidPaginationException exception = assertThrows(
            InvalidPaginationException.class,
            () -> movieService.getMoviesByGenre( "en", 28, pageable )
        );

        assertThat( exception.getMessage() )
            .isEqualTo( "Invalid page: Pages start at 1 and max at 500. They are expected to be an integer." );
    }

    @Test 
    void shouldThrownNotFoundException() {

        Pageable page = PageRequest.of(0,20);
        
        NotFoundException notFound = assertThrows(
            NotFoundException.class,
            () -> movieService.getMoviesByGenre( "en", 1000, page )
        );
        
        assertThat( notFound.getMessage() )
            .isEqualTo( "Genre does not exist" );
    }
    
    @Test 
    void shouldReturnTopRatedMoviesWhenCategoryIs100() {
        
        Pageable page = PageRequest.of(0, 20);
        List< MoviesProviderData > movies = new ArrayList<>();
        movies.add( new MoviesProviderData(
            1560520L,
            "Batman: Knightfall Part 1: Knightfall",
            "2026-06-23",
            9.158,
            "/360qdtu2hLnqMu8SVHMywn420w1.jpg",
            false
        ));
        
        TmdbMoviesPageableResponse prviderResult = new TmdbMoviesPageableResponse(
            1,
            500,
            10000,
            movies
        );
        when( movieProvider.getTopRatedMovies("en", 1))
            .thenReturn(prviderResult);
        
        MoviePageResponse response = movieService.getMoviesByGenre( "en", 100, page);
        
        assertThat( response.getMovies().get(0).getTitle() )
            .isEqualTo("Batman: Knightfall Part 1: Knightfall");
        
            verify( movieProvider ).getTopRatedMovies( "en", 1 );
    }
}
