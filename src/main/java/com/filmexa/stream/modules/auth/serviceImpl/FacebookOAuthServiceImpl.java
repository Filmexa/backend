/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   FacebookOAuthServiceImpl.java                      :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/11 20:04:53 by kchaouki          #+#    #+#             */
/*   Updated: 2026/09/11 20:48:01 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.auth.serviceImpl;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import tools.jackson.databind.JsonNode;
import com.filmexa.stream.modules.auth.dto.OAuthTokenResponse;
import com.filmexa.stream.modules.auth.dto.OAuthUserResponse;
import com.filmexa.stream.modules.auth.service.ProviderAuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Service
public class FacebookOAuthServiceImpl implements ProviderAuthService {

    private static final String STATE_SESSION_KEY = "oauth2_facebook_state";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${facebook.redirect-uri}")
    private String redirectUri;

    @Value("${facebook.client.id}")
    private String clientId;

    @Value("${facebook.client.secret}")
    private String clientSecret;

    @Value("${facebook.authorize-url}")
    private String authorizeUrl;

    @Value("${facebook.token-url}")
    private String tokenUrl;

    @Value("${facebook.userinfo-url}")
    private String userinfoUrl;

    @Override
    public String getAuthorizationUrl(HttpServletRequest request) {
        String state = UUID.randomUUID().toString();

        HttpSession session = request.getSession(true);
        session.setAttribute(STATE_SESSION_KEY, state);

        return UriComponentsBuilder.fromUriString(authorizeUrl)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "email public_profile")
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    @Override
    public OAuthUserResponse authenticate(String code, String state, HttpServletRequest request) throws IllegalStateException {
        validateState(state, request);

        String accessToken = exchangeCodeForAccessToken(code);
        return fetchFacebookUser(accessToken);
    }

    private void validateState(String state, HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new IllegalStateException("OAuth session not found");
        }

        String expectedState = (String) session.getAttribute(STATE_SESSION_KEY);
        session.removeAttribute(STATE_SESSION_KEY);

        if (expectedState == null || !expectedState.equals(state)) {
            throw new IllegalStateException("Invalid OAuth state");
        }
    }

    private String exchangeCodeForAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        try {
            OAuthTokenResponse tokenResponse = restTemplate.postForObject(
                    tokenUrl, new HttpEntity<>(body, headers), OAuthTokenResponse.class);

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                throw new IllegalStateException("Failed to obtain Facebook access token");
            }

            return tokenResponse.getAccessToken();
        } catch (RestClientException e) {
            throw new IllegalStateException("Failed to exchange code for Facebook access token", e);
        }
    }

    private OAuthUserResponse fetchFacebookUser(String accessToken) {
        String url = UriComponentsBuilder.fromUriString(userinfoUrl)
                .queryParam("fields", "id,email,first_name,last_name,picture")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        try {
            JsonNode me = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();

            if (me == null || me.path("id").asString(null) == null) {
                throw new IllegalStateException("Failed to retrieve Facebook user");
            }

            OAuthUserResponse facebookUser = new OAuthUserResponse();
            facebookUser.setProviderId(me.path("id").asString(null));
            facebookUser.setEmail(me.path("email").asString(null));
            facebookUser.setFirstName(me.path("first_name").asString(null));
            facebookUser.setLastName(me.path("last_name").asString(null));
            facebookUser.setImageUrl(me.path("picture").path("data").path("url").asString(null));

            return facebookUser;
        } catch (RestClientException e) {
            throw new IllegalStateException("Failed to retrieve Facebook user", e);
        }
    }
}
