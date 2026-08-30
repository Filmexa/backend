/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   RefreshTokenCookieServiceImpl.java                 :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/30 13:23:25 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/30 13:26:13 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.auth.serviceImpl;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.filmexa.stream.modules.auth.service.RefreshTokenCookieService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class RefreshTokenCookieServiceImpl implements RefreshTokenCookieService {

    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.name:filmexa_token}")
    private String cookieName;

    @Value("${security.jwt.refresh-expiration-time}")
    private long refreshExpirationMs;

    public void addCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ofMillis(refreshExpirationMs))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
