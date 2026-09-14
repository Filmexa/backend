/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieCategoryResponse.java                         :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 12:21:55 by kchaouki          #+#    #+#             */
/*   Updated: 2026/09/14 12:35:27 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.movieCategory.dto;

import java.util.UUID;

import lombok.Data;

@Data 
public class MovieCategoryResponse {

    private UUID id;
    private String name;
    private Long genreId;
}
