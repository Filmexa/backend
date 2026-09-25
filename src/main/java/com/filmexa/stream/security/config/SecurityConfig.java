/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   SecurityConfig.java                                :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/08 18:34:10 by kchaouki          #+#    #+#             */
/*   Updated: 2026/09/23 13:16:56 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.security.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.filmexa.stream.security.filter.JwtAuthenticationFilter;
import com.filmexa.stream.security.handler.RestAccessDeniedHandler;
import com.filmexa.stream.security.handler.RestAuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;

import org.springframework.web.client.RestClient;
import org.springframework.http.HttpHeaders;

@RequiredArgsConstructor
@Configuration
public class SecurityConfig {

	private static final String[] SWAGGER_WHITELIST = {
			"/swagger-ui.html",
			"/swagger-ui/**",
			"/v3/api-docs",
			"/v3/api-docs/**",
			"/v3/api-docs.yaml"
	};

	private static final String[] AUTH_WHITELIST = {
			"/api/auth/login",
			"/api/auth/register",
			"/api/auth/verify",
			"/api/auth/resend-verification",
			"/api/auth/forgot-password",
			"/api/auth/resend-password-reset",
			"/api/auth/reset-password",
			"/api/auth/refresh",
			"/api/auth/42",
			"/api/auth/42/callback",
			"/api/auth/google",
			"/api/auth/google/callback",
			"/api/auth/facebook",
			"/api/auth/facebook/callback"
	};

	private final UserDetailsService userDetailsService;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
	private final RestAccessDeniedHandler restAccessDeniedHandler;

	@Value("${app.cors.allowed-origins}")
	private List<String> allowedOrigins;
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> auth
						.requestMatchers(SWAGGER_WHITELIST).permitAll()
						.requestMatchers(AUTH_WHITELIST).permitAll()
						.requestMatchers("/error").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(restAuthenticationEntryPoint)
						.accessDeniedHandler(restAccessDeniedHandler))
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authenticationProvider(authenticationProvider())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setExposedHeaders(List.of("Retry-After"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	} 

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfiguration) throws Exception {
		return authConfiguration.getAuthenticationManager();
	}

	@Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
    
    @Bean
    RestClient tmdbRestClient(
            RestClient.Builder builder,
            @Value("${tmdb.base-url}") String baseUrl,
            @Value("${tmdb.access-token}") String token
    ) {
        return builder
                .baseUrl(baseUrl)
                .defaultHeader(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + token
                )
                .build();
    }

	@Bean
    RestClient YtsRestClient(
            RestClient.Builder builder,
            @Value("${yts.base-url}") String baseUrl
    ) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }

	@Bean
    RestClient PirateBayRestClient(
            RestClient.Builder builder,
            @Value("${piratebay.base-url}") String baseUrl
    ) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
