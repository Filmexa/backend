/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   AuthResponse.java                                  :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/14 15:43:19 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/14 21:53:52 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.auth.dto;

import lombok.Data;

@Data
public class AuthResponse {
    
    private String email;
    private String accessToken;
    private String refreshToken;
}
