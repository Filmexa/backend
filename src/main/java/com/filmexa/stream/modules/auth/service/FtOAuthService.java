/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   FtOAuthService.java                                :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/17 11:23:33 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/17 12:01:36 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.auth.service;

import org.springframework.stereotype.Service;

import com.filmexa.stream.modules.auth.dto.FtUserResponse;

import jakarta.servlet.http.HttpServletRequest;

@Service
public interface FtOAuthService {

    public String getAuthorizationUrl(HttpServletRequest request);

    public FtUserResponse authenticate(String code, String state, HttpServletRequest request) throws IllegalStateException;
}
