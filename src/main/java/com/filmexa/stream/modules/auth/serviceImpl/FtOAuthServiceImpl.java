/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   FtOAuthServiceImpl.java                            :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/17 11:24:43 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/17 13:06:50 by kchaouki         ###   ########.fr       */
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
import com.filmexa.stream.modules.auth.dto.FtTokenResponse;
import com.filmexa.stream.modules.auth.dto.FtUserResponse;
import com.filmexa.stream.modules.auth.service.FtOAuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Service
public class FtOAuthServiceImpl implements FtOAuthService {

    private static final String AUTHORIZE_URL = "https://api.intra.42.fr/oauth/authorize";
    private static final String TOKEN_URL = "https://api.intra.42.fr/oauth/token";
    private static final String ME_URL = "https://api.intra.42.fr/v2/me";
    private static final String STATE_SESSION_KEY = "oauth2_state";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ft.redirect-uri}")
    private String redirectUri;

    @Value("${OAUTH_42_CLIENT_ID}")
    private String clientId;

    @Value("${OAUTH_42_CLIENT_SECRET}")
    private String clientSecret;


    @Override
    public String getAuthorizationUrl(HttpServletRequest request) {
        String state = UUID.randomUUID().toString();

        HttpSession session = request.getSession(true);
        session.setAttribute(STATE_SESSION_KEY, state);

        return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "public")
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    @Override
    public FtUserResponse authenticate(String code, String state, HttpServletRequest request) throws IllegalStateException {
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
            FtTokenResponse tokenResponse = restTemplate.postForObject(
                    TOKEN_URL, new HttpEntity<>(body, headers), FtTokenResponse.class);

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                throw new IllegalStateException("Failed to obtain 42 access token");
            }

            return tokenResponse.getAccessToken();
        } catch (RestClientException e) {
            throw new IllegalStateException("Failed to exchange code for 42 access token", e);
        }
    }

    private FtUserResponse fetchFtUser(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        try {
            JsonNode me = restTemplate.exchange(
                    ME_URL, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();

            // print the response for debugging
            System.out.println("42 API response: " + me);

            if (me == null) {
                throw new IllegalStateException("Failed to retrieve 42 user");
            }

            FtUserResponse ftUser = new FtUserResponse();
            ftUser.setId(me.path("id").asLong());
            ftUser.setLogin(me.path("login").asString(null));
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
