package com.novaleap.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaleap.api.common.Result;
import com.novaleap.api.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final WebExpressionAuthorizationManager ADMIN_API_ACCESS =
            new WebExpressionAuthorizationManager(
                    "hasAuthority('PORTAL_ADMIN') and (hasRole('ADMIN') or hasRole('OPS') or hasRole('SUPER_ADMIN'))"
            );

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final String allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper,
            @Value("${nova.security.allowed-origins:*}") String allowedOrigins
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJsonIfNotCommitted(response, HttpServletResponse.SC_UNAUTHORIZED, Result.error(401, "请先登录"))
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJsonIfNotCommitted(response, HttpServletResponse.SC_FORBIDDEN, Result.error(403, "无访问权限"))
                        )
                )
                .authorizeHttpRequests(authz -> authz
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/guest",
                                "/api/auth/password/reset",
                                "/api/auth/email/send-code",
                                "/api/admin/auth/login"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/leaderboard/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/analytics/visit").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/questions", "/api/questions/categories", "/api/questions/random", "/api/questions/*", "/api/questions/*/answer").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/questions/*/view").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/wishes", "/api/wishes/*/comments").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/wishes/*/like").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ai/question/*/explain", "/api/ai/quote/daily").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/admin/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").access(ADMIN_API_ACCESS)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList());
        if (origins.isEmpty()) {
            throw new IllegalStateException(
                    "NOVA_ALLOWED_ORIGINS is not configured. Set it to your frontend domain(s) in production.");
        }
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeJson(HttpServletResponse response, int httpStatus, Result<?> body) throws java.io.IOException {
        response.setStatus(httpStatus);
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private void writeJsonIfNotCommitted(HttpServletResponse response, int httpStatus, Result<?> body) throws java.io.IOException {
        if (response.isCommitted()) {
            return;
        }
        writeJson(response, httpStatus, body);
    }
}
