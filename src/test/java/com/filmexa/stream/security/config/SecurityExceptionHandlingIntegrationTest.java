package com.filmexa.stream.security.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filmexa.stream.AbstractIntegrationTest;
import com.filmexa.stream.modules.notification.service.NotificationService;
import com.filmexa.stream.support.AuthTestSupport;
import com.filmexa.stream.support.AuthTestSupport.RegisteredUser;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pins the 401-vs-403 contract: a missing or unusable credential is 401, and the
 * whitelisted endpoints stay reachable without one.
 */
class SecurityExceptionHandlingIntegrationTest extends AbstractIntegrationTest {

    private static final String PROTECTED_ENDPOINT = "/api/users/me/profile";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void missingAuthorizationHeader_shouldReturn401WithErrorResponseBody() throws Exception {
        mockMvc.perform(get(PROTECTED_ENDPOINT))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void malformedAuthorizationHeader_shouldReturn401() throws Exception {
        mockMvc.perform(get(PROTECTED_ENDPOINT)
                        .header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void bearerPrefixWithEmptyToken_shouldReturn401() throws Exception {
        mockMvc.perform(get(PROTECTED_ENDPOINT)
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void tamperedToken_shouldReturn401() throws Exception {
        RegisteredUser user = AuthTestSupport.registerVerifyAndLogin(mockMvc, objectMapper,
                notificationService, "securityuser1", "securityuser1@example.com");

        String tampered = user.accessToken().substring(0, user.accessToken().length() - 2) + "xy";

        mockMvc.perform(get(PROTECTED_ENDPOINT)
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void unauthorizedResponse_shouldNotFallBackToBootErrorPage() throws Exception {
        mockMvc.perform(get(PROTECTED_ENDPOINT))
                .andExpect(status().isUnauthorized())
                .andExpect(content -> {
                    String body = content.getResponse().getContentAsString();
                    if (body.contains("\"path\"") || body.contains("\"timestamp\"")) {
                        throw new AssertionError("Expected ErrorResponse body, got Boot error page: " + body);
                    }
                })
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void validToken_shouldStillReachProtectedEndpoint() throws Exception {
        RegisteredUser user = AuthTestSupport.registerVerifyAndLogin(mockMvc, objectMapper,
                notificationService, "securityuser2", "securityuser2@example.com");

        mockMvc.perform(get(PROTECTED_ENDPOINT)
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("securityuser2"));
    }

    @Test
    void whitelistedEndpoint_shouldNotRequireAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void unknownPathWithoutToken_shouldReturn401RatherThan404() throws Exception {
        // anyRequest().authenticated() runs before dispatch, so authentication wins.
        mockMvc.perform(get("/api/does-not-exist")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
