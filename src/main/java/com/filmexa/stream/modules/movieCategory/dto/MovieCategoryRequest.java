/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieCategoryRequest.java                          :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 12:21:55 by kchaouki          #+#    #+#             */
/*   Updated: 2026/09/22 12:28:57 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.movieCategory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data 
public class MovieCategoryRequest {

    @NotBlank(message = "English name is required")
    @Size(max = 100, message = "English name must not exceed 100 characters")
    private String nameEn;

    @Size(max = 100, message = "French name must not exceed 100 characters")
    private String nameFr;

    @Size(max = 100, message = "Arabic name must not exceed 100 characters")
    private String nameAr;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Genre ID is required")
    @Positive(message = "Genre ID must be greater than 0")
    private Long genreId;

    @PositiveOrZero(message = "Display order must be zero or greater")
    private Integer displayOrder;
}
