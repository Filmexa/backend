/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   JwtAuthenticationFilter.java                       :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/14 13:05:28 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/14 21:33:49 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.security.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.filmexa.stream.modules.streaming.security.StreamTokenService;
import com.filmexa.stream.security.service.JwtService;
import com.filmexa.stream.security.service.UserDetailsServiceImpl;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsServiceImpl;
    private final StreamTokenService streamTokenService;

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsServiceImpl,
            StreamTokenService streamTokenService) {
        this.jwtService = jwtService;
        this.userDetailsServiceImpl = userDetailsServiceImpl;
        this.streamTokenService = streamTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // hls.js cannot set headers on playlist/segment requests, so streaming falls
            // back to the short-lived stream token in the query string.
            authenticateStreamToken(request);
            filterChain.doFilter(request, response);
        } else {
            String token = authHeader.substring(7);

            try {
                final String username = jwtService.extractUsername(token);

                if (username != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                    UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(username);
                    if (jwtService.isTokenValid(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                            );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
                // IllegalArgumentException covers a blank token ("Bearer " with nothing
                // after it); leaving the context empty lets the entry point answer 401.
                SecurityContextHolder.clearContext();
            }

            filterChain.doFilter(request, response);
        }
    }

    private void authenticateStreamToken(HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        String username = streamTokenService.validate(request.getParameter("token"));
        if (username == null) {
            return;
        }

        try {
            UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (UsernameNotFoundException e) {
            SecurityContextHolder.clearContext();
        }
    }
}
