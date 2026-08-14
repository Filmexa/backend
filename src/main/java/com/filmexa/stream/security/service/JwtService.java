/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   JwtService.java                                    :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/10 15:21:56 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/14 21:33:51 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.security.service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.filmexa.stream.modules.users.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    
    @Value("${security.jwt.secret-key}")
    private String JWT_SECRET;

    @Value("${security.jwt.expiration-time}")
    private long JWT_EXPIRATION;

    @Value("${security.jwt.refresh-expiration-time}")
    private long JWT_REFRESH_EXPIRATION;

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getId() + ", " + user.getUsername() + ", " + user.getRole())
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
                .signWith(getSignedKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(user.getId() + ", " + user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + JWT_REFRESH_EXPIRATION))
                .claim("type", "refresh")
                .signWith(getSignedKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSignedKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationTime() {
        return JWT_EXPIRATION;
    }

    public String extractUsername(String token) {
        String subject = extractClaim(token, Claims::getSubject);
        String[] parts = subject.split(", ");
        if (parts.length >= 2) {
            return parts[1];
        }
        return null;
    }

    public String extractRole(String token) {
        String subject = extractClaim(token, Claims::getSubject);
        String[] parts = subject.split(", ");
        if (parts.length >= 3) {
            return parts[2];
        }
        return null;
    }

    public boolean validateRefreshToken(String token) {
        try {
            return isRefreshToken(token) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isRefreshToken(String token) {
        try {
            String type = extractClaim(token, claims -> claims.get("type", String.class));
            return "refresh".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()))
                && !isTokenExpired(token)
                && !isRefreshToken(token);
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    private Key getSignedKey() {
        byte[] keyBytes = Decoders.BASE64.decode(JWT_SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignedKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}
