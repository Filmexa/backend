/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   AvatarService.java                                 :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/16 18:12:21 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/16 19:10:55 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.users.service;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.filmexa.stream.modules.users.dto.AvatarFile;

public interface AvatarService {

    AvatarFile loadAvatar(String pictureUrl);

    void saveAvatar(UUID userId, MultipartFile file);
}
