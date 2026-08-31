/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   UserProfileResponse.java                           :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/16 19:20:00 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/31 11:57:19 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.users.dto;

import java.util.UUID;

import com.filmexa.stream.modules.users.enums.PreferredLanguage;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileResponse {

    private UUID id;

    private String username;

    private String firstName;

    private String lastName;

    private String email;

    private String phoneNumber;

    private String avatarUrl;

    private PreferredLanguage preferredLanguage;
}
