/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   CastProviderData.java                              :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/17 23:20:37 by maddou            #+#    #+#             */
/*   Updated: 2026/09/17 23:22:12 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.dto.tmdb;


import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CastProviderData {
    Integer     id;
    String      name;
    String      profile_path;
    String      character;        
}
