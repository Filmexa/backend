package com.filmexa.stream.modules.users.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filmexa.stream.AbstractIntegrationTest;
import com.filmexa.stream.modules.notification.service.NotificationService;
import com.filmexa.stream.support.AuthTestSupport;
import com.filmexa.stream.support.AuthTestSupport.RegisteredUser;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

class AvatarIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private NotificationService notificationService;

    private RegisteredUser registerUser(String username) throws Exception {
        return AuthTestSupport.registerVerifyAndLogin(mockMvc, objectMapper, notificationService,
                username, username + "@example.com");
    }

    @Test
    void uploadAvatar_thenRetrieveIt_viaOwnEndpointAndPublicEndpoint() throws Exception {
        RegisteredUser user = registerUser("avataruser1");
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "fake-png-bytes".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/profile")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl", notNullValue()));

        mockMvc.perform(get("/api/users/me/avatar")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes("fake-png-bytes".getBytes(StandardCharsets.UTF_8)));

        String profileJson = mockMvc.perform(get("/api/users/me/profile")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andReturn().getResponse().getContentAsString();
        UUID userId = UUID.fromString(objectMapper.readTree(profileJson).get("id").asText());

        mockMvc.perform(get("/api/users/" + userId + "/avatar")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void uploadAvatar_shouldReplacePreviousFile() throws Exception {
        RegisteredUser user = registerUser("avataruser2");

        MockMultipartFile firstFile = new MockMultipartFile(
                "file", "first.png", "image/png", "first-bytes".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/users/me/avatar")
                        .file(firstFile)
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isOk());

        MockMultipartFile secondFile = new MockMultipartFile(
                "file", "second.jpg", "image/jpeg", "second-bytes".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/users/me/avatar")
                        .file(secondFile)
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/avatar")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes("second-bytes".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void uploadAvatar_shouldReturnBadRequest_whenExtensionNotAllowed() throws Exception {
        RegisteredUser user = registerUser("avataruser3");
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", "data".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadAvatar_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "bytes".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/users/me/avatar").file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void getAvatar_shouldReturnNotFound_whenUserHasNoAvatar() throws Exception {
        RegisteredUser user = registerUser("avataruser4");

        mockMvc.perform(get("/api/users/me/avatar")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isNotFound());
    }
}
