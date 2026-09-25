package com.filmexa.stream.security.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class RestAuthenticationEntryPointTest {

    private final RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void commence_shouldWriteUnauthorizedJsonBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("no token"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("message").asText()).isNotBlank();
    }

    @Test
    void commence_shouldAdvertiseBearerScheme() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest(), response,
                new InsufficientAuthenticationException("no token"));

        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
    }
}
