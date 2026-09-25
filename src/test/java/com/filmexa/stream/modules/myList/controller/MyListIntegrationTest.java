package com.filmexa.stream.modules.myList.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filmexa.stream.AbstractIntegrationTest;
import com.filmexa.stream.modules.moviesExternal.client.MovieProvider;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieProvederData;
import com.filmexa.stream.modules.notification.service.NotificationService;
import com.filmexa.stream.support.AuthTestSupport;
import com.filmexa.stream.support.AuthTestSupport.RegisteredUser;

class MyListIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private MovieProvider movieProvider;

    @BeforeEach
    void stubMovieProvider() {
        when(movieProvider.getMovieById(anyString(), anyInt())).thenAnswer(invocation -> {
            Integer id = invocation.getArgument(1);
            MovieProvederData movie = new MovieProvederData();
            movie.setId(id);
            movie.setTitle("Movie " + id);
            movie.setRelease_date("1999-10-15");
            movie.setVote_average(8.4);
            movie.setPoster_path("/poster-" + id + ".jpg");
            return movie;
        });
    }

    private RegisteredUser registerUser(String username) throws Exception {
        return AuthTestSupport.registerVerifyAndLogin(mockMvc, objectMapper, notificationService,
                username, username + "@example.com");
    }

    @Test
    void addAndListMyList_shouldReturnNewestFirst() throws Exception {
        RegisteredUser user = registerUser("lister1");

        mockMvc.perform(post("/api/my-list")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"movieId":550}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movieId").value(550))
                .andExpect(jsonPath("$.title").value("Movie 550"));

        mockMvc.perform(post("/api/my-list")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"movieId":680}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/my-list")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].movieId").value(680))
                .andExpect(jsonPath("$.content[1].movieId").value(550));
    }

    @Test
    void addToMyList_shouldReturnConflict_whenAlreadyAdded() throws Exception {
        RegisteredUser user = registerUser("lister2");

        mockMvc.perform(post("/api/my-list")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"movieId":550}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/my-list")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"movieId":550}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void addToMyList_shouldReturnBadRequest_whenMovieIdMissing() throws Exception {
        RegisteredUser user = registerUser("lister3");

        mockMvc.perform(post("/api/my-list")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void myList_shouldBeScopedToTheOwner() throws Exception {
        RegisteredUser owner = registerUser("lister4");
        RegisteredUser other = registerUser("lister5");

        mockMvc.perform(post("/api/my-list")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"movieId":603}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/my-list")
                        .header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        // Another user cannot remove an entry they do not own.
        mockMvc.perform(delete("/api/my-list/603")
                        .header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/my-list/603")
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/my-list")
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void myList_shouldReturnUnauthorized_whenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/my-list")
                        .header("Authorization", "Bearer " + "not valide token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
