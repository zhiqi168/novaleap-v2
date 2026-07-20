package com.novaleap.api.security;

import com.novaleap.api.module.auth.support.AuthPortal;
import com.novaleap.api.module.auth.support.AuthTokenStateSupport;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtils jwtUtils;
    private final AuthTokenStateSupport authTokenStateSupport;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, AuthTokenStateSupport authTokenStateSupport) {
        this.jwtUtils = jwtUtils;
        this.authTokenStateSupport = authTokenStateSupport;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtils.parseToken(token);
                String username = claims.getSubject();
                Long issuedAtMillis = claims.get("iat_ms", Long.class);
                if (!authTokenStateSupport.isTokenActive(username, claims.getIssuedAt(), issuedAtMillis)) {
                    SecurityContextHolder.clearContext();
                    chain.doFilter(request, response);
                    return;
                }

                String role = claims.get("role", String.class);
                AuthPortal portal = AuthPortal.fromClaim(claims.get("portal", String.class));
                if (StringUtils.hasText(username)) {
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + (StringUtils.hasText(role) ? role : "USER")));
                    authorities.add(new SimpleGrantedAuthority(portal.authority()));

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            authorities
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                log.warn("JWT validation failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
