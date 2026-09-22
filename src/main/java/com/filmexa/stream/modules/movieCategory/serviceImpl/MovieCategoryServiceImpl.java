/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieCategoryServiceImpl.java                      :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 11:14:16 by kchaouki          #+#    #+#             */
/*   Updated: 2026/09/22 12:38:03 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.movieCategory.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.filmexa.stream.common.exception.ConflictException;
import com.filmexa.stream.common.exception.NotFoundException;
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

        checkUniqueness(movieCategory.getGenreId(), movieCategory.getNameEn(),
                movieCategory.getNameFr(), movieCategory.getNameAr(), null);

        return movieCategoryRepository.save(movieCategory);
    }

    @Override 
    public MovieCategory createMovieCategory(MovieCategoryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("MovieCategory cannot be null");
        }

        checkUniqueness(request.getGenreId(), request.getNameEn(),
                blankToNull(request.getNameFr()), blankToNull(request.getNameAr()), null);

        MovieCategory entity = new MovieCategory();
        entity.setNameEn(request.getNameEn());
        entity.setNameFr(blankToNull(request.getNameFr()));
        entity.setNameAr(blankToNull(request.getNameAr()));
        entity.setDescription(request.getDescription());
        entity.setGenreId(request.getGenreId());
        entity.setActive(true);
        entity.setDisplayOrder(request.getDisplayOrder());

        return movieCategoryRepository.save(entity);
    }

    @Override
    public MovieCategory updateMovieCategory(MovieCategory movieCategory) {
        if (movieCategory == null) {
            throw new IllegalArgumentException("MovieCategory cannot be null");
        }

        checkUniqueness(movieCategory.getGenreId(), movieCategory.getNameEn(),
                movieCategory.getNameFr(), movieCategory.getNameAr(), movieCategory.getId());

        movieCategory.setUpdatedAt(LocalDateTime.now());
        return movieCategoryRepository.save(movieCategory);
    }

    @Override
    public MovieCategory updateMovieCategory(UUID id, MovieCategoryRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("MovieCategory cannot be null");
        }

        MovieCategory entity = movieCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("MovieCategory not found"));

        checkUniqueness(request.getGenreId(), request.getNameEn(),
                blankToNull(request.getNameFr()), blankToNull(request.getNameAr()), id);

        entity.setNameEn(request.getNameEn());
        entity.setNameFr(blankToNull(request.getNameFr()));
        entity.setNameAr(blankToNull(request.getNameAr()));
        entity.setDescription(request.getDescription());
        entity.setGenreId(request.getGenreId());
        entity.setDisplayOrder(request.getDisplayOrder());
        entity.setUpdatedAt(LocalDateTime.now());

        return movieCategoryRepository.save(entity);
    }

    @Override
    public void deleteMovieCategory(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        if (!movieCategoryRepository.existsById(id)) {
            throw new NotFoundException("MovieCategory not found");
        }
        movieCategoryRepository.deleteById(id);
    }

    @Override 
    public List<MovieCategoryResponse> getAllMovieCategoriesResponse(String language) {
        return movieCategoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(category -> {
                    MovieCategoryResponse response = new MovieCategoryResponse();
                    response.setId(category.getId());
                    response.setName(category.getName(language));
                    response.setGenreId(category.getGenreId());
                    return response;
                })
                .collect(Collectors.toList());
    }

    private void checkUniqueness(Long genreId, String nameEn, String nameFr,
            String nameAr, UUID currentId) {

        if (genreId == null) {
            throw new IllegalArgumentException("Genre ID cannot be null");
        }

        requireFree(movieCategoryRepository::findByGenreId, genreId, currentId,
                "A movie category with genre ID " + genreId + " already exists");

        requireFree(movieCategoryRepository::findByNameEn, nameEn, currentId,
                "A movie category with the English name '" + nameEn + "' already exists");

        requireFree(movieCategoryRepository::findByNameFr, nameFr, currentId,
                "A movie category with the French name '" + nameFr + "' already exists");

        requireFree(movieCategoryRepository::findByNameAr, nameAr, currentId,
                "A movie category with the Arabic name '" + nameAr + "' already exists");
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private <T> void requireFree(Function<T, Optional<MovieCategory>> finder, T value,
            UUID currentId, String message) {

        if (value == null) {
            return;
        }

        finder.apply(value)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ConflictException(message);
                });
    }
}
