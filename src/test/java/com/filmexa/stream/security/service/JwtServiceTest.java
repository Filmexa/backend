package com.filmexa.stream.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import com.filmexa.stream.modules.users.enums.AuthProvider;
import com.filmexa.stream.modules.users.enums.PreferredLanguage;
import com.filmexa.stream.modules.users.enums.Role;

class JwtServiceTest {

    private static final String SECRET_KEY = "d96ac79641c2dbc2a040e31ad167b7baa8157008c19d62fb9de0af75355adad7";

    private JwtService jwtService;
    private com.filmexa.stream.modules.users.entity.User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "JWT_SECRET", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "JWT_EXPIRATION", 3600000L);
        ReflectionTestUtils.setField(jwtService, "JWT_REFRESH_EXPIRATION", 604800000L);

        user = new com.filmexa.stream.modules.users.entity.User();
        user.setId(UUID.randomUUID());
        user.setUsername("johndoe");
        user.setEmail("johndoe@example.com");
        user.setHashedPassword("hashed");
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setPreferredLanguage(PreferredLanguage.ENGLISH);
        user.setRole(Role.USER);
        user.setEnabled(true);
    }

    @Test
    void generateToken_shouldContainUsernameAndBeValid() {
        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("johndoe");
        assertThat(jwtService.extractRole(token)).isEqualTo("USER");
    }

    @Test
    void generateRefreshToken_shouldBeValidatedAsRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(user);

        assertThat(jwtService.validateRefreshToken(refreshToken)).isTrue();
        assertThat(jwtService.extractUsername(refreshToken)).isEqualTo("johndoe");
    }

    @Test
    void validateRefreshToken_shouldReturnFalseForAccessToken() {
        String accessToken = jwtService.generateToken(user);

        assertThat(jwtService.validateRefreshToken(accessToken)).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnTrueForMatchingUserDetailsAndAccessToken() {
        String token = jwtService.generateToken(user);
        org.springframework.security.core.userdetails.UserDetails userDetails =
                User.withUsername("johndoe").password("hashed").authorities("USER").build();

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalseForDifferentUsername() {
        String token = jwtService.generateToken(user);
        org.springframework.security.core.userdetails.UserDetails userDetails =
                User.withUsername("someoneelse").password("hashed").authorities("USER").build();

        assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalseForRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(user);
        org.springframework.security.core.userdetails.UserDetails userDetails =
                User.withUsername("johndoe").password("hashed").authorities("USER").build();

        assertThat(jwtService.isTokenValid(refreshToken, userDetails)).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalseForBlankToken() {
        assertThat(jwtService.validateToken("")).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "JWT_EXPIRATION", -1000L);
        String expiredToken = jwtService.generateToken(user);

        assertThat(jwtService.validateToken(expiredToken)).isFalse();
    }

    @Test
    void getExpirationTime_shouldReturnConfiguredValue() {
        assertThat(jwtService.getExpirationTime()).isEqualTo(3600000L);
    }
}
