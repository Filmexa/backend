/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieServiceImpl.java                              :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 12:33:45 by maddou            #+#    #+#             */
/*   Updated: 2026/09/22 15:02:11 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.serviceImpl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.filmexa.stream.common.exception.NotFoundException;
import com.filmexa.stream.modules.moviesExternal.service.MovieService;
import com.filmexa.stream.modules.moviesExternal.client.MovieProvider;
import com.filmexa.stream.modules.moviesExternal.dto.response.MovieResponse;
import com.filmexa.stream.modules.moviesExternal.dto.request.MovieSearchQuery;
import com.filmexa.stream.modules.moviesExternal.dto.response.ActorResponse;
import com.filmexa.stream.modules.moviesExternal.dto.response.MovieDetailsResponse;
import com.filmexa.stream.modules.moviesExternal.dto.response.MoviePageResponse;
import com.filmexa.stream.modules.moviesExternal.dto.response.TrendingMoviesResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieDetailsProviderData;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieDetailsProviderResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.request.TmdbMovieDiscoverRequest;
import com.filmexa.stream.modules.moviesExternal.enums.MovieSort;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.TmdbMoviesPageableResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieProvederData;
import com.filmexa.stream.modules.moviesExternal.mapper.MovieGenreMapper;
import com.filmexa.stream.modules.moviesExternal.mapper.MovieMapper;

import com.filmexa.stream.common.exception.InvalidPaginationException;

@Service
public class MovieServiceImpl implements MovieService {

    private final MovieProvider movieProvider;
    private final MovieMapper   movieMapper;
    
    @Value("${tmdb.base-image-url}")
    private String imageBaseUrl;
    
    public MovieServiceImpl( MovieProvider movieProvider,
        MovieMapper movieMapper
     ) {
        this.movieProvider = movieProvider;
        this.movieMapper = movieMapper;
    }

    @Override
    public List< TrendingMoviesResponse > getTrendingMovies( String language) {

        List< MovieDetailsProviderResponse > providerMovies = movieProvider.getTrendingMovies( language );
        return providerMovies.stream()
        .limit(10)
        .map(movie -> new TrendingMoviesResponse(
            movie.getId(),
            movie.getTitle(),
            movie.getRelease_date(),
            movie.getPoster_path() != null ? this.imageBaseUrl + movie.getPoster_path() : "",
            movie.getBackdrop_path() != null ? this.imageBaseUrl + movie.getBackdrop_path() : "",
            movie.getOverview(),
            movie.getGenre_ids()
                .stream()
                .map( id -> MovieGenreMapper.getName( id, language ) )
                .toList()
            ))
        .toList();
    }

    @Override
    public Map<String, List<MovieResponse>> buildHomeMovies( String language ) {
        // get top rated
        List< MovieResponse > topRated = movieProvider.getTopRatedMovies( language )
            .stream()
            .map( movie -> this.movieMapper.toMovieResponse( movie ) )
            .toList();
        
        // get movies by genre
        List< MovieResponse > action = movieProvider.getMoviesByGenre( language, 28L )
            .stream()
            .map( movie -> this.movieMapper.toMovieResponse( movie ) )
            .toList();
        
        List< MovieResponse > comedy = movieProvider.getMoviesByGenre( language, 35L )
            .stream()
            .map( movie -> this.movieMapper.toMovieResponse( movie ) )
            .toList();
        
        List< MovieResponse > horror = movieProvider.getMoviesByGenre( language, 27L )
            .stream()
            .map( movie -> this.movieMapper.toMovieResponse( movie ) )
            .toList();
        
        List< MovieResponse > drama = movieProvider.getMoviesByGenre( language, 18L )
            .stream()
            .map( movie -> this.movieMapper.toMovieResponse( movie ) )
            .toList();
        
        List< MovieResponse > romance = movieProvider.getMoviesByGenre( language, 10749L )
            .stream()
            .map( movie -> this.movieMapper.toMovieResponse( movie ) )
            .toList();
        return this.generateHomeMoviesData(
            topRated,
            action,
            comedy,
            horror,
            drama,
            romance,
            language
        );
    }

    public List<MovieResponse> getMoviesByGenre( String language, Long id ) {
        List< MovieResponse > movies = movieProvider.getMoviesByGenre( language, id )
            .stream()
            .map( movie -> this.movieMapper.toMovieResponse( movie ) )
            .toList();
        return movies;
    }
    
    public MoviePageResponse getMoviesByGenre( String language, Integer id, Pageable page ) {
        int pageNumber = page.getPageNumber() == 0 ? 1 : page.getPageNumber();
        if ( pageNumber > 500 || pageNumber < 1 ) {
            throw new InvalidPaginationException( "Invalid page: Pages start at 1 and max at 500. They are expected to be an integer." );
        }
        // check genre does not exist
        String genre = MovieGenreMapper.getName( id, language );
        if ( genre == null ) {
            throw new NotFoundException( "Genre does not exist" );
        }
        TmdbMoviesPageableResponse providerResult = null;
        if ( id == 100 ) {
            providerResult = movieProvider.getTopRatedMovies( language, pageNumber );
        }
        else 
            providerResult = movieProvider.getMoviesByGenre( language, id, pageNumber );
        MoviePageResponse result = new MoviePageResponse(
            providerResult.getPage(),
            500,
            10000,
            providerResult.getResults()
                .stream()
                .map( movie -> this.movieMapper.toMovieResponse( movie ) )
                .toList()
        );
        return result;
    }
    @Override
    public MoviePageResponse searchMovie( MovieSearchQuery query, Pageable page ) {
        // validate page
        int pageNumber = page.getPageNumber() == 0 ? 1 : page.getPageNumber();
        if ( pageNumber > 500 || pageNumber < 1 ) {
            throw new InvalidPaginationException( "Invalid page: Pages start at 1 and max at 500. They are expected to be an integer." );
        }
        // IF --> user send to me title use search api provider before filtring using application code 
        if ( query.getQuery() == null ) {
            TmdbMovieDiscoverRequest providerRequest = new TmdbMovieDiscoverRequest(
                query.getLanguage(),
                query.getGenreId(),
                query.getYear(),
                query.getMinRating(),
                mapSort( query.getSortBy()),
                pageNumber
            );
            TmdbMoviesPageableResponse movies = movieProvider.discoverMovies( providerRequest );
            return new MoviePageResponse(
                movies.getPage(),
                movies.getTotal_pages(),
                movies.getResults().size(),
                movies.getResults()
                    .stream()
                    .map( movie -> this.movieMapper.toMovieResponse( movie ) )
                    .toList()
            );
            
        }
        List<MovieDetailsProviderData> movies = movieProvider.searchMovieByQuery( 
               query.getLanguage(), 
               query.getQuery(), 
               query.getYear(), 
               pageNumber 
        );
        MoviePageResponse filtredMovies = this.filterMovies(
            movies,
            query
        );
        
        return filtredMovies;
    }

    @Override
    public MovieDetailsResponse getMovieById( String language, Integer id ) {

        // Fetch details from provider 
        MovieProvederData movieDetails = this.movieProvider.getMovieById( language, id ); 
        // convert to application actors and limited to 10
        List< ActorResponse > actors =   movieDetails.getCredits().getCast()
            .stream()
            .limit(10)
            .map( movie -> new ActorResponse(
                movie.getId(),
                movie.getName(),
                movie.getProfile_path() != null ? this.imageBaseUrl + movie.getProfile_path() : null,
                movie.getCharacter()
            )).toList();
        List< String > genres = movieDetails.getGenres()
            .stream()
            .map( genre -> genre.getName() )
            .toList();
        // generate movies details application 
        return new MovieDetailsResponse(
            movieDetails.getId(),
            movieDetails.getBackdrop_path() != null ? this.imageBaseUrl + movieDetails.getBackdrop_path() : null,
            genres,
            movieDetails.getOverview(),
            movieDetails.getRelease_date(),
            movieDetails.getTitle(),
            movieDetails.getImdb_id(),
            movieDetails.getVote_average(),
            actors
        );
    }

    private Map<String, List<MovieResponse>> generateHomeMoviesData(
        List< MovieResponse > topRated,
        List< MovieResponse > action,
        List< MovieResponse > comedy,
        List< MovieResponse > horror,
        List< MovieResponse > drama,
        List< MovieResponse > romance,
        String  language
    ) {
        Map<String, List<MovieResponse>> homeMovies = new LinkedHashMap<>();
        homeMovies.put( MovieGenreMapper.getName( 100, language ), topRated );
        homeMovies.put( MovieGenreMapper.getName( 28, language ), action );
        homeMovies.put( MovieGenreMapper.getName( 35, language ), comedy );
        homeMovies.put( MovieGenreMapper.getName( 27, language ), horror );
        homeMovies.put( MovieGenreMapper.getName( 18, language ), drama );
        homeMovies.put( MovieGenreMapper.getName( 10749, language ), romance );
        return homeMovies;
    }
    
    private String mapSort(MovieSort sort) {
        if (sort == null) {
            return "popularity.desc";
        }
    
        return switch (sort) {
            case POPULARITY -> "popularity.desc";
            case RATING -> "vote_average.desc";
            case RELEASE_DATE -> "primary_release_date.desc";
        };
    }

    private MoviePageResponse filterMovies(
            List<MovieDetailsProviderData> movies,
            MovieSearchQuery query
    ) {
        List<MovieDetailsProviderData> filteredMovies = movies
            .stream()
            .filter(movie -> query.getGenreId() == null
                    || movie.getGenreIds() != null
                    && movie.getGenreIds().contains( query.getGenreId() ))

            .filter(movie -> query.getYear() == null
                    || movie.getReleaseDate() != null
                    && movie.getReleaseDate().startsWith(
                            query.getYear().toString()
                    ))

            .filter(movie -> query.getMinRating() == null
                    || movie.getRating() != null
                    && movie.getRating() >= query.getMinRating())

            .toList();

        return new MoviePageResponse(
        1,
        1,
        filteredMovies.size(),
        filteredMovies.stream()
                .map(movieProvider -> new MovieResponse(
                        movieProvider.getId(),
                        movieProvider.getTitle(),
                        movieProvider.getReleaseDate(),
                        movieProvider.getPosterPath() != null
                                ? this.imageBaseUrl + movieProvider.getPosterPath()
                                : null
                ))
                .toList()
);
    }
}
