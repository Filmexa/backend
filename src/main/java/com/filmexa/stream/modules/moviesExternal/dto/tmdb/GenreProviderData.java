/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   GenreProviderData.java                             :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/17 23:10:21 by maddou@stud       #+#    #+#             */
/*   Updated: 2026/09/17 23:11:03 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.dto.tmdb;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenreProviderData {
    Integer id;
    String name;
}
