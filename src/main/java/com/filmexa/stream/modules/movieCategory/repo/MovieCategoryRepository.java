/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieCategoryRepository.java                       :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 11:16:13 by kchaouki          #+#    #+#             */
/*   Updated: 2026/09/14 13:05:27 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.movieCategory.repo;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.filmexa.stream.modules.movieCategory.entity.MovieCategory;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Repository 
public interface MovieCategoryRepository extends JpaRepository<MovieCategory, UUID>, JpaSpecificationExecutor<MovieCategory> {
    List<MovieCategory> findAllByActiveTrueOrderByDisplayOrderAsc();
    Optional<MovieCategory> findByName(String name);
    Optional<MovieCategory> findByGenreId(Long genreId);
}
