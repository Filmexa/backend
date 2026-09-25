package com.filmexa.stream.modules.download.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filmexa.stream.AbstractIntegrationTest;
import com.filmexa.stream.modules.download.worker.TorrentDownloadWorker;
import com.filmexa.stream.modules.notification.service.NotificationService;
import com.filmexa.stream.support.AuthTestSupport;
import com.filmexa.stream.support.AuthTestSupport.RegisteredUser;

class DownloadIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private NotificationService notificationService;

    /**
     * Keeps the test off the network: the real worker would start a BitTorrent runtime,
     * bind its ports and go looking for peers. The controller, service and repository are
     * all still the real thing.
     */
    @MockitoBean
    private TorrentDownloadWorker torrentDownloadWorker;

    private String token;

    @BeforeEach
    void authenticate() throws Exception {
        // The schema lives as long as the context, so every test needs its own account -
        // reusing one name makes the second registration a 409.
        String username = "downloaduser" + UUID.randomUUID().toString().substring(0, 8);
        RegisteredUser user = AuthTestSupport.registerVerifyAndLogin(mockMvc, objectMapper,
                notificationService, username, username + "@example.com");
        token = "Bearer " + user.accessToken();
    }

    @Test
    void testStartDownloadAndGetProgress_shouldReturnAcceptedAndProgress() throws Exception {
        String requestJson = """
                {
                    "movieId": 27205,
                    "magnetUrl": "magnet:?xt=urn:btih:dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c&dn=Big+Buck+Bunny.mp4"
                }
                """;

        // 1. Test POST /api/downloads (Should return 202 Accepted)
        mockMvc.perform(post("/api/downloads")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.movieId").value(27205))
                .andExpect(jsonPath("$.status").exists());

        // 2. Test GET /api/downloads/{movieId}/progress (Should return 200 OK)
        mockMvc.perform(get("/api/downloads/27205/progress")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieId").value(27205))
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void testGetProgress_nonExistentMovie_shouldReturnNotFound() throws Exception {
        // Query progress for a movie ID that has never been downloaded
        mockMvc.perform(get("/api/downloads/999999/progress")
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void testStartDownload_invalidPayload_shouldReturnBadRequest() throws Exception {
        // Missing magnetUrl and missing movieId
        String invalidJson = """
                {
                    "movieId": null,
                    "magnetUrl": ""
                }
                """;

        mockMvc.perform(post("/api/downloads")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testStopDownload_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/downloads/27205/stop")
                        .header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    void testDownloadEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/downloads/27205/progress"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/downloads/27205/stop"))
                .andExpect(status().isUnauthorized());
    }
}
