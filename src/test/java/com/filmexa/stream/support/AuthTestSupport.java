package com.filmexa.stream.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filmexa.stream.modules.notification.service.NotificationService;
import com.filmexa.stream.modules.users.entity.User;

import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;


public final class AuthTestSupport {

    private AuthTestSupport() {
    }

    public record RegisteredUser(String username, String email, String accessToken) {
    }

    public static RegisteredUser registerVerifyAndLogin(MockMvc mockMvc, ObjectMapper objectMapper,
            NotificationService notificationService, String username, String email) throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "Passw0rd!",
                                  "firstName": "John",
                                  "lastName": "Doe"
                                }
                                """.formatted(username, email)))
                .andExpect(status().isCreated());

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService, atLeastOnce())
                .sendVerificationCode(any(User.class), codeCaptor.capture(), anyLong());
        String code = codeCaptor.getValue();

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"%s"}
                                """.formatted(email, code)))
                .andExpect(status().isOk());

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Passw0rd!"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(loginResponse).get("accessToken").asText();
        return new RegisteredUser(username, email, accessToken);
    }
}
