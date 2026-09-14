/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieCategoryRequest.java                          :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 12:21:55 by kchaouki          #+#    #+#             */
/*   Updated: 2026/09/14 12:35:59 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.movieCategory.dto;

import lombok.Data;

@Data 
public class MovieCategoryRequest {

    private String name;
    private String description;
    private Long genreId;
    private Integer displayOrder;
}
