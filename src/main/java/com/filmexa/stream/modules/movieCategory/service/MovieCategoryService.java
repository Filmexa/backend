/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieCategoryService.java                          :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 11:14:05 by kchaouki          #+#    #+#             */
/*   Updated: 2026/09/14 12:46:00 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.movieCategory.service;

import org.springframework.stereotype.Service;

import com.filmexa.stream.modules.movieCategory.dto.MovieCategoryRequest;
import com.filmexa.stream.modules.movieCategory.dto.MovieCategoryResponse;
import com.filmexa.stream.modules.movieCategory.entity.MovieCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service 
public interface MovieCategoryService {

    List<MovieCategory> getAllMovieCategories();

    List<MovieCategoryResponse> getAllMovieCategoriesResponse();

    Optional<MovieCategory> getMovieCategoryById(UUID id);

    MovieCategory createMovieCategory(MovieCategory movieCategory);

    MovieCategory createMovieCategory(MovieCategoryRequest movieCategory);

    MovieCategory updateMovieCategory(MovieCategory movieCategory);

    void deleteMovieCategory(UUID id);
}
