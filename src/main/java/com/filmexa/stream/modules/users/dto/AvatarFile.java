/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   AvatarFile.java                                    :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/16 18:12:21 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/16 19:10:46 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.users.dto;

import org.springframework.http.MediaType;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AvatarFile {

    private byte[] data;

    private MediaType contentType;
}
