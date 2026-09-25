package com.filmexa.stream.modules.download.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.filmexa.stream.AbstractIntegrationTest;

class DownloadIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.movieId").value(27205))
                .andExpect(jsonPath("$.status").exists());

        // 2. Test GET /api/downloads/{movieId}/progress (Should return 200 OK)
        mockMvc.perform(get("/api/downloads/27205/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieId").value(27205))
                .andExpect(jsonPath("$.status").exists());
    }

        @Test
    void testGetProgress_nonExistentMovie_shouldReturnNotFound() throws Exception {
        // Query progress for a movie ID that has never been downloaded
        mockMvc.perform(get("/api/downloads/999999/progress"))
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testStopDownload_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/downloads/27205/stop"))
                .andExpect(status().isOk());
    }
}
