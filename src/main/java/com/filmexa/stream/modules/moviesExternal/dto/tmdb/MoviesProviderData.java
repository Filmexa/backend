/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MoviesProviderData.java                                    :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/15 18:26:39 by marouan           #+#    #+#             */
/*   Updated: 2026/09/15 18:27:02 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.dto.tmdb;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MoviesProviderData {
    Long    id;
    String  title;
    String  release_date;
    Double  vote_average;
    String  poster_path;
}
