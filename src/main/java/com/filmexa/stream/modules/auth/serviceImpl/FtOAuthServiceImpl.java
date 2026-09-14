/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   FtOAuthServiceImpl.java                            :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/17 11:24:43 by kchaouki          #+#    #+#             */
/*   Updated: 2026/09/13 16:47:05 by kchaouki         ###   ########.fr       */
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
public class FtOAuthServiceImpl implements ProviderAuthService {

    private static final String STATE_SESSION_KEY = "oauth2_state";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ft.redirect-uri}")
    private String redirectUri;

    @Value("${ft.client.id}")
    private String clientId;

    @Value("${ft.client.secret}")
    private String clientSecret;

    @Value("${ft.authorize-url}")
    private String authorizeUrl;

    @Value("${ft.token-url}")
    private String tokenUrl;

    @Value("${ft.me-url}")
    private String meUrl;


    @Override
    public String getAuthorizationUrl(HttpServletRequest request) {
        String state = UUID.randomUUID().toString();

        HttpSession session = request.getSession(true);
        session.setAttribute(STATE_SESSION_KEY, state);

        return UriComponentsBuilder.fromUriString(authorizeUrl)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "public")
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    @Override
    public OAuthUserResponse authenticate(String code, String state, HttpServletRequest request) throws IllegalStateException {
        validateState(state, request);

        String accessToken = exchangeCodeForAccessToken(code);
        return fetchFtUser(accessToken);
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
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        try {
            OAuthTokenResponse tokenResponse = restTemplate.postForObject(
                    tokenUrl, new HttpEntity<>(body, headers), OAuthTokenResponse.class);

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                throw new IllegalStateException("Failed to obtain 42 access token");
            }

            return tokenResponse.getAccessToken();
        } catch (RestClientException e) {
            throw new IllegalStateException("Failed to exchange code for 42 access token", e);
        }
    }

    private OAuthUserResponse fetchFtUser(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        try {
            JsonNode me = restTemplate.exchange(
                    meUrl, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();

            if (me == null) {
                throw new IllegalStateException("Failed to retrieve 42 user");
            }

            OAuthUserResponse ftUser = new OAuthUserResponse();
            ftUser.setProviderId(String.valueOf(me.path("id").asLong()));
            ftUser.setUsername(me.path("login").asString(null));
            ftUser.setEmail(me.path("email").asString(null));
            ftUser.setFirstName(me.path("first_name").asString(null));
            ftUser.setLastName(me.path("last_name").asString(null));
            ftUser.setPhone(me.path("phone").asString(null));
            ftUser.setImageUrl(me.path("image").path("link").asString(null));

            return ftUser;
        } catch (RestClientException e) {
            throw new IllegalStateException("Failed to retrieve 42 user", e);
        }
    }
}
