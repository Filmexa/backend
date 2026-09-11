package com.filmexa.stream.modules.auth.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
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

class FacebookOAuthServiceImplTest {

    private static final String STATE_SESSION_KEY = "oauth2_facebook_state";

    private FacebookOAuthServiceImpl facebookOAuthService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        facebookOAuthService = new FacebookOAuthServiceImpl();
        ReflectionTestUtils.setField(facebookOAuthService, "redirectUri", "http://localhost:8080/api/auth/facebook/callback");
        ReflectionTestUtils.setField(facebookOAuthService, "clientId", "client-id");
        ReflectionTestUtils.setField(facebookOAuthService, "clientSecret", "client-secret");
        ReflectionTestUtils.setField(facebookOAuthService, "authorizeUrl", "https://www.facebook.com/v19.0/dialog/oauth");
        ReflectionTestUtils.setField(facebookOAuthService, "tokenUrl", "https://graph.facebook.com/v19.0/oauth/access_token");
        ReflectionTestUtils.setField(facebookOAuthService, "userinfoUrl", "https://graph.facebook.com/me");

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(facebookOAuthService, "restTemplate");
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void getAuthorizationUrl_shouldBuildUrlAndStoreStateInSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        String url = facebookOAuthService.getAuthorizationUrl(request);

        assertThat(url).startsWith("https://www.facebook.com/v19.0/dialog/oauth");
        assertThat(url).contains("client_id=client-id");
        assertThat(url).contains("scope=email");

        String storedState = (String) request.getSession(false).getAttribute(STATE_SESSION_KEY);
        assertThat(storedState).isNotBlank();
        assertThat(url).contains("state=" + storedState);
    }

    @Test
    void authenticate_shouldThrow_whenNoSessionExists() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> facebookOAuthService.authenticate("code", "some-state", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OAuth session not found");
    }

    @Test
    void authenticate_shouldThrow_whenStateDoesNotMatch() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(STATE_SESSION_KEY, "expected-state");
        request.setSession(session);

        assertThatThrownBy(() -> facebookOAuthService.authenticate("code", "wrong-state", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid OAuth state");
    }

    @Test
    void authenticate_shouldReturnUser_whenStateValidAndProviderRespondsSuccessfully() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(STATE_SESSION_KEY, "correct-state");
        request.setSession(session);

        mockServer.expect(requestToUriTemplate("https://graph.facebook.com/v19.0/oauth/access_token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"access_token":"abc123","token_type":"bearer"}
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestToUriTemplate(
                        "https://graph.facebook.com/me?fields=id,email,first_name,last_name,picture"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "122132070272736923",
                          "email": "jdoe@example.com",
                          "first_name": "John",
                          "last_name": "Doe",
                          "picture": {
                            "data": {
                              "url": "https://scontent.fcmn1-4.fna.fbcdn.net/v/t1.30497-1/84628273_176.jpg"
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        OAuthUserResponse result = facebookOAuthService.authenticate("auth-code", "correct-state", request);

        assertThat(result.getProviderId()).isEqualTo("122132070272736923");
        assertThat(result.getEmail()).isEqualTo("jdoe@example.com");
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getImageUrl()).isEqualTo("https://scontent.fcmn1-4.fna.fbcdn.net/v/t1.30497-1/84628273_176.jpg");
        mockServer.verify();
    }

    @Test
    void authenticate_shouldThrow_whenUserInfoMissingId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(STATE_SESSION_KEY, "correct-state");
        request.setSession(session);

        mockServer.expect(requestToUriTemplate("https://graph.facebook.com/v19.0/oauth/access_token"))
                .andRespond(withSuccess("""
                        {"access_token":"abc123","token_type":"bearer"}
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestToUriTemplate(
                        "https://graph.facebook.com/me?fields=id,email,first_name,last_name,picture"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> facebookOAuthService.authenticate("auth-code", "correct-state", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to retrieve Facebook user");
    }
}
