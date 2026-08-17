/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   RefreshTokenRequest.java                           :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/14 20:54:47 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/14 21:49:16 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.auth.dto;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    String refreshToken;
}
