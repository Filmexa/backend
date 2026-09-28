/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TmdbMovieProviderTest.java                         :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/28 15:23:57 by maddou            #+#    #+#             */
/*   Updated: 2026/09/28 16:35:26 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.filmexa.stream.common.exception.ExternalServiceException;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieDetailsProviderResponse;

import org.springframework.http.MediaType;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import java.util.List;

public class TmdbMovieProviderTest {

    private static final String TMDB_BASE_URL =
        "https://api.themoviedb.org/3";
        
    private TmdbMovieProvider movieProvider;

    private MockRestServiceServer server;
    
    @BeforeEach 
    void setup() {
        RestClient.Builder builder = RestClient.builder().baseUrl(TMDB_BASE_URL);
        server = MockRestServiceServer
            .bindTo(builder)
            .build();
        RestClient restClient = builder.build();
        
        movieProvider = new TmdbMovieProvider(restClient);
    }

    @Test 
    void shouldReturnTrendingMovies( ) {

        server.expect(
            requestTo( 
                TMDB_BASE_URL + "/trending/movie/week?language=en&include_adult=false"
            ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """
                    {
                        "results": [
                            {
                                "id": 1,
                                "title": "Spider-Man: Brand New Day",
                                "release_date": "2026-07-29",
                                "poster_path": "/bjiS5ipwxb9JFy3XRRN4OAilSeX.jpg",
                                "backdrop_path": "/bjiS5ipwxb9JFy3XRRN4OAilSeX.jpg",
                                "overview": "test",
                                "vote_average": 7.864,
                                "genre_ids": [27, 878, 12],
                                "adult": false
                            }
                        ]
                    }
                    """,
                    MediaType.APPLICATION_JSON
                )
        );
        
        List< MovieDetailsProviderResponse > result = movieProvider.getTrendingMovies( "en");
        
        assertThat( result ).hasSize(1);
        assertThat( result.get(0).getTitle()).
            isEqualTo("Spider-Man: Brand New Day");
        
        server.verify();
    }

    @Test 
    void shouldTHrownExternalServiceException( ) {
        server.expect(requestTo( 
                TMDB_BASE_URL + "/trending/movie/week?language=en&include_adult=false"
            )).andExpect( method(HttpMethod.GET))
            .andRespond(withServerError());
        
        ExternalServiceException error = assertThrows(
            ExternalServiceException.class,
            () -> movieProvider.getTrendingMovies( "en" )
        );
        
        assertThat( error.getMessage() )
            .isEqualTo("External service is unavailable");
        
        server.verify();
    }

    @Test 
    void souldRestunEmptyArray( ) {
        server.expect(
            requestTo( TMDB_BASE_URL + "/trending/movie/week?language=en&include_adult=false")
        ).andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess());

        List< MovieDetailsProviderResponse > response = movieProvider.getTrendingMovies("en");
        
        assertThat(response).isEmpty();
        server.verify();
    }
}
