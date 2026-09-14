/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieService.java                                  :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 12:33:09 by maddou            #+#    #+#             */
/*   Updated: 2026/09/14 20:21:58 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.service;

import java.util.List;

import com.filmexa.stream.modules.moviesExternal.dto.response.TrendingMoviesResponse;

public interface MovieService {
    List< TrendingMoviesResponse > getTrendingMovie( ); 
}
