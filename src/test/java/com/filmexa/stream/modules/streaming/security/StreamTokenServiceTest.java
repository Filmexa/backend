package com.filmexa.stream.modules.streaming.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.filmexa.stream.modules.streaming.config.StreamProperties;

class StreamTokenServiceTest {

    private static final String SECRET = "d96ac79641c2dbc2a040e31ad167b7baa8157008c19d62fb9de0af75355adad7";

    private StreamProperties properties;
    private StreamTokenService service;

    @BeforeEach
    void setUp() {
        properties = new StreamProperties();
        properties.setTokenSecret(SECRET);
        properties.setTokenTtlMinutes(180);
        service = new StreamTokenService(properties);
    }

    @Test
    void aFreshTokenResolvesBackToItsUser() {
        String token = service.issue("johndoe");

        assertThat(service.validate(token)).isEqualTo("johndoe");
    }

    @Test
    void anExpiredTokenIsRejected() {
        properties.setTokenTtlMinutes(-1);

        String token = service.issue("johndoe");

        assertThat(service.validate(token)).isNull();
    }

    @Test
    void garbageAndMissingTokensAreRejectedRatherThanThrowing() {
        assertThat(service.validate(null)).isNull();
        assertThat(service.validate("")).isNull();
        assertThat(service.validate("not-a-jwt")).isNull();
    }

    @Test
    void aRegularAccessTokenIsNotAcceptedAsAStreamToken() {
        // Same signing key, but no "stream" type claim.
        String foreign = io.jsonwebtoken.Jwts.builder()
                .setSubject("johndoe")
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        io.jsonwebtoken.io.Decoders.BASE64.decode(SECRET)),
                        io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        assertThat(service.validate(foreign)).isNull();
    }
}
