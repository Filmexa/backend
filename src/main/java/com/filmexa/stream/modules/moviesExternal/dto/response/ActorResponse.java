/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   ActorResponse.java                                 :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/17 23:32:04 by maddou            #+#    #+#             */
/*   Updated: 2026/09/17 23:35:41 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.dto.response;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActorResponse {
    Integer     id;
    String      name;
    String      profile;
    String      character; 
}
