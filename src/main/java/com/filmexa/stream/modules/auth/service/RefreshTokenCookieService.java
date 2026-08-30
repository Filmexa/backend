/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   RefreshTokenCookieService.java                     :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/30 13:21:42 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/30 13:26:09 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.auth.service;

import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public interface RefreshTokenCookieService {

    public void addCookie(HttpServletResponse response, String refreshToken);

    public void clearCookie(HttpServletResponse response);

    public String extractToken(HttpServletRequest request);
}
