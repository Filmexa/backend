package com.filmexa.stream.modules.streaming.security;

import java.security.Key;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.filmexa.stream.modules.streaming.config.StreamProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

/**
 * Issues and validates short-lived stream tokens.
 *
 * <p>hls.js cannot attach an {@code Authorization} header to its playlist and segment
 * requests, so the normal bearer token is useless there. The client exchanges it for one
 * of these and carries it in the query string instead. A query string leaks easily - into
 * browser history, access logs, Referer headers - which is why this token is deliberately
 * weak: it only works on the streaming endpoints, and only for a few hours.
 */
@Service
@RequiredArgsConstructor
public class StreamTokenService {

    private static final String TYPE_CLAIM = "type";
    private static final String TOKEN_TYPE = "stream";

    private final StreamProperties properties;

    public String issue(String username) {
        Date now = new Date();
        long ttlMillis = properties.getTokenTtlMinutes() * 60_000L;

        return Jwts.builder()
                .setSubject(username)
                .claim(TYPE_CLAIM, TOKEN_TYPE)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ttlMillis))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public long getTtlSeconds() {
        return properties.getTokenTtlMinutes() * 60L;
    }

    /**
     * @return the username the token was issued to, or null when it is invalid, expired,
     *         or is an ordinary access token rather than a stream token
     */
    public String validate(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if (!TOKEN_TYPE.equals(claims.get(TYPE_CLAIM, String.class))) {
                return null;
            }
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    private Key signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getTokenSecret()));
    }
}
