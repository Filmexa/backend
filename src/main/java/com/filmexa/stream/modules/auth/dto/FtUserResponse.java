/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   FtUserResponse.java                                :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/17 11:26:08 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/17 12:34:30 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class FtUserResponse {
    
    private Long id;
    private String login;
    private String email;

    private String firstName;

    private String lastName;

    @JsonProperty("image_url")
    private String imageUrl;

    private String phone;
}
