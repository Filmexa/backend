/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieCategoryServiceImpl.java                      :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 11:14:16 by kchaouki          #+#    #+#             */
/*   Updated: 2026/09/14 13:12:23 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.movieCategory.serviceImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.filmexa.stream.modules.movieCategory.entity.MovieCategory;
import com.filmexa.stream.modules.movieCategory.repo.MovieCategoryRepository;
import com.filmexa.stream.modules.movieCategory.service.MovieCategoryService;
import com.filmexa.stream.modules.movieCategory.dto.MovieCategoryRequest;
import com.filmexa.stream.modules.movieCategory.dto.MovieCategoryResponse;
import java.util.stream.Collectors;

@Service 
public class MovieCategoryServiceImpl implements MovieCategoryService {

    private final MovieCategoryRepository movieCategoryRepository;

    public MovieCategoryServiceImpl(MovieCategoryRepository movieCategoryRepository) {
        this.movieCategoryRepository = movieCategoryRepository;
    }

    @Override
    public List<MovieCategory> getAllMovieCategories() {
        return movieCategoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc();
    }

    @Override
    public Optional<MovieCategory> getMovieCategoryById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        return movieCategoryRepository.findById(id);
    }

    @Override
    public MovieCategory createMovieCategory(MovieCategory movieCategory) {
        if (movieCategory == null) {
            throw new IllegalArgumentException("MovieCategory cannot be null");
        }

        if (movieCategoryRepository.findByName(movieCategory.getName()).isPresent()) {
            throw new IllegalArgumentException("MovieCategory with the same name already exists");
        }

        if (movieCategoryRepository.findByGenreId(movieCategory.getGenreId()) == null) {
            throw new IllegalArgumentException("Genre ID cannot be null");
        }

        return movieCategoryRepository.save(movieCategory);
    }

    @Override 
    public MovieCategory createMovieCategory(MovieCategoryRequest movieCategory) {
        if (movieCategory == null) {
            throw new IllegalArgumentException("MovieCategory cannot be null");
        }

        if (movieCategoryRepository.findByName(movieCategory.getName()).isPresent()) {
            throw new IllegalArgumentException("MovieCategory with the same name already exists");
        }

        if (movieCategoryRepository.findByGenreId(movieCategory.getGenreId()) == null) {
            throw new IllegalArgumentException("Genre ID cannot be null");
        }

        MovieCategory entity = new MovieCategory();
        entity.setName(movieCategory.getName());
        entity.setGenreId(movieCategory.getGenreId());
        entity.setActive(true);
        entity.setDisplayOrder(movieCategory.getDisplayOrder());

        entity = movieCategoryRepository.save(entity);
        return entity;
    }

    @Override
    public MovieCategory updateMovieCategory(MovieCategory movieCategory) {
        return movieCategoryRepository.save(movieCategory);
    }

    @Override
    public void deleteMovieCategory(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        movieCategoryRepository.deleteById(id);
    }

    @Override 
    public List<MovieCategoryResponse> getAllMovieCategoriesResponse() {
        return movieCategoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(category -> {
                    MovieCategoryResponse response = new MovieCategoryResponse();
                    response.setId(category.getId());
                    response.setName(category.getName());
                    response.setGenreId(category.getGenreId());
                    return response;
                })
                .collect(Collectors.toList());
    }

}
