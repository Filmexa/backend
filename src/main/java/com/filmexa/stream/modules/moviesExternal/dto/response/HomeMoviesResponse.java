/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   HomeMoviesResponse.java                            :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/16 12:10:40 by marouan           #+#    #+#             */
/*   Updated: 2026/09/16 13:11:48 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.dto.response;

import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;


import com.filmexa.stream.modules.moviesExternal.dto.response.MovieResponse;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomeMoviesResponse {
    Map<String, List<MovieResponse>> response;
}
