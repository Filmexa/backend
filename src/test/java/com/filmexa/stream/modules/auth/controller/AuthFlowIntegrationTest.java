package com.filmexa.stream.modules.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filmexa.stream.AbstractIntegrationTest;
import com.filmexa.stream.modules.notification.service.NotificationService;
import com.filmexa.stream.modules.users.entity.User;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private NotificationService notificationService;

    private String registerAndCaptureVerificationCode(String username, String email) throws Exception {
        String registerBody = """
                {
                  "username": "%s",
                  "email": "%s",
                  "password": "Passw0rd!",
                  "firstName": "John",
                  "lastName": "Doe"
                }
                """.formatted(username, email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendVerificationCode(any(User.class), codeCaptor.capture(), anyLong());
        return codeCaptor.getValue();
    }

    @Test
    void fullFlow_registerVerifyLoginAndAccessProtectedEndpoint() throws Exception {
        String username = "flowuser1";
        String email = "flowuser1@example.com";

        String code = registerAndCaptureVerificationCode(username, email);
        assertThat(code).matches("\\d{6}");

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"%s"}
                                """.formatted(email, code)))
                .andExpect(status().isOk());

        MockHttpServletResponse loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Passw0rd!"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andReturn().getResponse();

        var authResponse = objectMapper.readTree(loginResult.getContentAsString());
        String accessToken = authResponse.get("accessToken").asText();
        Cookie refreshCookie = loginResult.getCookie("filmexa_token");
        assertThat(refreshCookie).isNotNull();

        mockMvc.perform(get("/api/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()));
    }

    @Test
    void register_shouldReturnConflict_whenUsernameAlreadyTaken() throws Exception {
        registerAndCaptureVerificationCode("duplicateuser", "duplicateuser@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "duplicateuser",
                                  "email": "different@example.com",
                                  "password": "Passw0rd!"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void login_shouldReturnForbidden_whenAccountNotVerified() throws Exception {
        registerAndCaptureVerificationCode("unverifieduser", "unverifieduser@example.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"unverifieduser","password":"Passw0rd!"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void verify_shouldReturnBadRequest_whenCodeIsWrong() throws Exception {
        String email = "wrongcodeuser@example.com";
        registerAndCaptureVerificationCode("wrongcodeuser", email);

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"000000"}
                                """.formatted(email)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpoint_shouldReturnForbidden_whenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/api/users/me/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_shouldReturnForbidden_whenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/users/me/profile")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isForbidden());
    }
}
