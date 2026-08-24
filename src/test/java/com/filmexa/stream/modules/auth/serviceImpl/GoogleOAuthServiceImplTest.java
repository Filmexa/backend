package com.filmexa.stream.modules.auth.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.filmexa.stream.modules.auth.dto.OAuthUserResponse;

class GoogleOAuthServiceImplTest {

    private static final String STATE_SESSION_KEY = "oauth2_google_state";

    private GoogleOAuthServiceImpl googleOAuthService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        googleOAuthService = new GoogleOAuthServiceImpl();
        ReflectionTestUtils.setField(googleOAuthService, "redirectUri", "http://localhost:8080/api/auth/google/callback");
        ReflectionTestUtils.setField(googleOAuthService, "clientId", "client-id");
        ReflectionTestUtils.setField(googleOAuthService, "clientSecret", "client-secret");
        ReflectionTestUtils.setField(googleOAuthService, "authorizeUrl", "https://accounts.google.com/o/oauth2/v2/auth");
        ReflectionTestUtils.setField(googleOAuthService, "tokenUrl", "https://oauth2.googleapis.com/token");
        ReflectionTestUtils.setField(googleOAuthService, "userinfoUrl", "https://www.googleapis.com/oauth2/v3/userinfo");

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(googleOAuthService, "restTemplate");
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void getAuthorizationUrl_shouldBuildUrlAndStoreStateInSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        String url = googleOAuthService.getAuthorizationUrl(request);

        assertThat(url).startsWith("https://accounts.google.com/o/oauth2/v2/auth");
        assertThat(url).contains("client_id=client-id");
        assertThat(url).contains("scope=openid");

        String storedState = (String) request.getSession(false).getAttribute(STATE_SESSION_KEY);
        assertThat(storedState).isNotBlank();
        assertThat(url).contains("state=" + storedState);
    }

    @Test
    void authenticate_shouldThrow_whenNoSessionExists() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> googleOAuthService.authenticate("code", "some-state", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OAuth session not found");
    }

    @Test
    void authenticate_shouldThrow_whenStateDoesNotMatch() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(STATE_SESSION_KEY, "expected-state");
        request.setSession(session);

        assertThatThrownBy(() -> googleOAuthService.authenticate("code", "wrong-state", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid OAuth state");
    }

    @Test
    void authenticate_shouldReturnUser_whenStateValidAndProviderRespondsSuccessfully() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(STATE_SESSION_KEY, "correct-state");
        request.setSession(session);

        mockServer.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"access_token":"abc123","token_type":"bearer"}
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://www.googleapis.com/oauth2/v3/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "sub": "1234567890",
                          "email": "jdoe@gmail.com",
                          "given_name": "John",
                          "family_name": "Doe",
                          "picture": "https://lh3.googleusercontent.com/jdoe.jpg"
                        }
                        """, MediaType.APPLICATION_JSON));

        OAuthUserResponse result = googleOAuthService.authenticate("auth-code", "correct-state", request);

        assertThat(result.getProviderId()).isEqualTo("1234567890");
        assertThat(result.getEmail()).isEqualTo("jdoe@gmail.com");
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getImageUrl()).isEqualTo("https://lh3.googleusercontent.com/jdoe.jpg");
        mockServer.verify();
    }

    @Test
    void authenticate_shouldThrow_whenUserInfoMissingSub() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(STATE_SESSION_KEY, "correct-state");
        request.setSession(session);

        mockServer.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(withSuccess("""
                        {"access_token":"abc123","token_type":"bearer"}
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://www.googleapis.com/oauth2/v3/userinfo"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> googleOAuthService.authenticate("auth-code", "correct-state", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to retrieve Google user");
    }
}
