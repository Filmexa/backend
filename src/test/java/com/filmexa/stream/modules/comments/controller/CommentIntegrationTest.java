package com.filmexa.stream.modules.comments.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filmexa.stream.AbstractIntegrationTest;
import com.filmexa.stream.modules.notification.service.NotificationService;
import com.filmexa.stream.support.AuthTestSupport;
import com.filmexa.stream.support.AuthTestSupport.RegisteredUser;

class CommentIntegrationTest extends AbstractIntegrationTest {

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
    void createAndListComments_shouldReturnNewestFirst() throws Exception {
        RegisteredUser user = registerUser("commenter1");

        mockMvc.perform(post("/api/movies/101/comments")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"First comment"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("First comment"))
                .andExpect(jsonPath("$.movieId").value(101))
                .andExpect(jsonPath("$.author.username").value("commenter1"));

        mockMvc.perform(post("/api/movies/101/comments")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Second comment"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/movies/101/comments")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("Second comment"))
                .andExpect(jsonPath("$.content[1].content").value("First comment"));
    }

    @Test
    void createComment_shouldReturnBadRequest_whenContentBlank() throws Exception {
        RegisteredUser user = registerUser("commenter2");

        mockMvc.perform(post("/api/movies/101/comments")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"   "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAndDelete_shouldAllowAuthorOnly() throws Exception {
        RegisteredUser author = registerUser("commenter3");
        RegisteredUser other = registerUser("commenter4");

        String created = mockMvc.perform(post("/api/movies/202/comments")
                        .header("Authorization", "Bearer " + author.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Original"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String commentId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(patch("/api/movies/202/comments/" + commentId)
                        .header("Authorization", "Bearer " + other.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Hijacked"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/movies/202/comments/" + commentId)
                        .header("Authorization", "Bearer " + author.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Edited"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Edited"));

        mockMvc.perform(delete("/api/movies/202/comments/" + commentId)
                        .header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/movies/202/comments/" + commentId)
                        .header("Authorization", "Bearer " + author.accessToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/movies/202/comments")
                        .header("Authorization", "Bearer " + author.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void createComment_shouldReturnForbidden_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/movies/101/comments")
                        .header("Authorization", "Bearer " + "not valide token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Nope"}
                                """))
                .andExpect(status().isForbidden());
    }
}
