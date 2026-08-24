package com.filmexa.stream.modules.auth.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.filmexa.stream.modules.auth.dto.OAuthUserResponse;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

class FtOAuthServiceImplTest {

    private static final String STATE_SESSION_KEY = "oauth2_state";

    private FtOAuthServiceImpl ftOAuthService;
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        ftOAuthService = new FtOAuthServiceImpl();
        ReflectionTestUtils.setField(ftOAuthService, "redirectUri", "http://localhost:8080/api/auth/42/callback");
        ReflectionTestUtils.setField(ftOAuthService, "clientId", "client-id");
        ReflectionTestUtils.setField(ftOAuthService, "clientSecret", "client-secret");
        ReflectionTestUtils.setField(ftOAuthService, "authorizeUrl", "https://api.intra.42.fr/oauth/authorize");
        ReflectionTestUtils.setField(ftOAuthService, "tokenUrl", "https://api.intra.42.fr/oauth/token");
        ReflectionTestUtils.setField(ftOAuthService, "meUrl", "https://api.intra.42.fr/v2/me");

        restTemplate = (RestTemplate) ReflectionTestUtils.getField(ftOAuthService, "restTemplate");
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void getAuthorizationUrl_shouldBuildUrlAndStoreStateInSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        String url = ftOAuthService.getAuthorizationUrl(request);

        assertThat(url).startsWith("https://api.intra.42.fr/oauth/authorize");
        assertThat(url).contains("client_id=client-id");
        assertThat(url).contains("redirect_uri=");
        assertThat(url).contains("scope=public");

        String storedState = (String) request.getSession(false).getAttribute(STATE_SESSION_KEY);
        assertThat(storedState).isNotBlank();
        assertThat(url).contains("state=" + storedState);
    }

    @Test
    void authenticate_shouldThrow_whenNoSessionExists() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> ftOAuthService.authenticate("code", "some-state", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OAuth session not found");
    }

    @Test
    void authenticate_shouldThrow_whenStateDoesNotMatch() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(STATE_SESSION_KEY, "expected-state");
        request.setSession(session);

        assertThatThrownBy(() -> ftOAuthService.authenticate("code", "wrong-state", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid OAuth state");
    }

    @Test
    void authenticate_shouldReturnUser_whenStateValidAndProviderRespondsSuccessfully() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(STATE_SESSION_KEY, "correct-state");
        request.setSession(session);

        mockServer.expect(requestTo("https://api.intra.42.fr/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"access_token":"abc123","token_type":"bearer"}
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://api.intra.42.fr/v2/me"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": 42,
                          "login": "jdoe",
                          "email": "jdoe@student.1337.ma",
                          "first_name": "John",
                          "last_name": "Doe",
                          "phone": "+212600000000",
                          "image": {"link": "https://cdn.intra.42.fr/jdoe.jpg"}
                        }
                        """, MediaType.APPLICATION_JSON));

        OAuthUserResponse result = ftOAuthService.authenticate("auth-code", "correct-state", request);

        assertThat(result.getProviderId()).isEqualTo("42");
        assertThat(result.getUsername()).isEqualTo("jdoe");
        assertThat(result.getEmail()).isEqualTo("jdoe@student.1337.ma");
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getImageUrl()).isEqualTo("https://cdn.intra.42.fr/jdoe.jpg");
        mockServer.verify();
    }

    @Test
    void authenticate_shouldThrow_whenTokenResponseMissingAccessToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(STATE_SESSION_KEY, "correct-state");
        request.setSession(session);

        mockServer.expect(requestTo("https://api.intra.42.fr/oauth/token"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> ftOAuthService.authenticate("auth-code", "correct-state", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to obtain 42 access token");
    }
}
