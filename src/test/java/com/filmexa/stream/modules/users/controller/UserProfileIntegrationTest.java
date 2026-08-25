package com.filmexa.stream.modules.users.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filmexa.stream.AbstractIntegrationTest;
import com.filmexa.stream.modules.notification.service.NotificationService;
import com.filmexa.stream.modules.users.entity.User;
import com.filmexa.stream.support.AuthTestSupport;
import com.filmexa.stream.support.AuthTestSupport.RegisteredUser;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

class UserProfileIntegrationTest extends AbstractIntegrationTest {

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
    void updateMyProfile_shouldPersistOnlyProvidedFields() throws Exception {
        RegisteredUser user = registerUser("profileuser1");

        mockMvc.perform(patch("/api/users/me/profile")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Jane"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Doe"));

        mockMvc.perform(get("/api/users/me/profile")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.username").value("profileuser1"));
    }

    @Test
    void updateMyProfile_shouldReturnBadRequest_whenPhoneNumberInvalid() throws Exception {
        RegisteredUser user = registerUser("profileuser2");

        mockMvc.perform(patch("/api/users/me/profile")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber":"not-a-phone!!"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMyProfile_shouldReturnForbidden_whenNotAuthenticated() throws Exception {
        mockMvc.perform(patch("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Jane"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserProfile_shouldReturnPublicProfile_byUserId() throws Exception {
        RegisteredUser user = registerUser("profileuser3");

        String meJson = mockMvc.perform(get("/api/users/me/profile")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andReturn().getResponse().getContentAsString();
        String userId = objectMapper.readTree(meJson).get("id").asText();

        mockMvc.perform(get("/api/users/" + userId + "/profile")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("profileuser3"));
    }

    @Test
    void getAllProfiles_shouldReturnPagedResults() throws Exception {
        registerUser("profileuser4");
        RegisteredUser user = registerUser("profileuser5");

        mockMvc.perform(get("/api/users/profiles")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void changePreferredLanguage_shouldUpdateLanguage() throws Exception {
        RegisteredUser user = registerUser("profileuser6");

        mockMvc.perform(post("/api/users/change-preferred-language")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"preferredLanguage":"FRENCH"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void changeEmail_thenConfirm_shouldUpdateEmailAddress() throws Exception {
        RegisteredUser user = registerUser("profileuser7");

        mockMvc.perform(post("/api/users/change-email")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newEmail":"newmail-profileuser7@example.com"}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendEmailChangeCode(any(User.class),
                eq("newmail-profileuser7@example.com"), codeCaptor.capture(), anyLong());
        String code = codeCaptor.getValue();

        mockMvc.perform(post("/api/users/change-email/confirm")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s"}
                                """.formatted(code)))
                .andExpect(status().isOk());
    }

    @Test
    void changeEmail_shouldReturnConflict_whenEmailAlreadyRegistered() throws Exception {
        registerUser("profileuser8");
        RegisteredUser secondUser = registerUser("profileuser9");

        mockMvc.perform(post("/api/users/change-email")
                        .header("Authorization", "Bearer " + secondUser.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newEmail":"profileuser8@example.com"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void confirmChangeEmail_shouldReturnBadRequest_whenCodeIsWrong() throws Exception {
        RegisteredUser user = registerUser("profileuser10");

        mockMvc.perform(post("/api/users/change-email")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newEmail":"another-profileuser10@example.com"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/change-email/confirm")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"000000"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
