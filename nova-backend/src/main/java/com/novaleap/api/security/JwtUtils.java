package com.novaleap.api.security;

import com.novaleap.api.module.auth.support.AuthPortal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${nova.jwt.secret}")
    private String secret;

    @Value("${nova.jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username, String role) {
        return generateToken(username, role, AuthPortal.CLIENT);
    }

    public String generateToken(String username, String role, AuthPortal portal) {
        long issuedAtMillis = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("portal", portal.claimValue())
                .claim("iat_ms", issuedAtMillis)
                .issuedAt(new Date(issuedAtMillis))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String getPortalFromToken(String token) {
        return parseClaims(token).get("portal", String.class);
    }

    public Date getIssuedAtFromToken(String token) {
        return parseClaims(token).getIssuedAt();
    }

    public Claims parseToken(String token) {
        return parseClaims(token);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
