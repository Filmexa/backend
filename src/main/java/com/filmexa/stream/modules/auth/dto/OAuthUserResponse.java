/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   OAuthUserResponse.java                             :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/17 15:22:16 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/17 15:22:17 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.auth.dto;

import lombok.Data;

@Data
public class OAuthUserResponse {

    private String providerId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String imageUrl;
    private String phone;
}
