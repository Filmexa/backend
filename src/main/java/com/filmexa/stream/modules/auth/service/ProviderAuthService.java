/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   ProviderAuthService.java                           :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/17 15:15:33 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/17 15:26:59 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.auth.service;

import org.springframework.stereotype.Service;

import com.filmexa.stream.modules.auth.dto.OAuthUserResponse;

import jakarta.servlet.http.HttpServletRequest;

@Service
public interface ProviderAuthService {

    public String getAuthorizationUrl(HttpServletRequest request);

    public OAuthUserResponse authenticate(String code, String state, HttpServletRequest request) throws IllegalStateException;
}
