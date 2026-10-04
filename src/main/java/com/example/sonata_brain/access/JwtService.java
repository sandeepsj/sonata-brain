package com.example.sonata_brain.access;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * Issues and checks JWTs. Tokens carry only the user's email and a type (access or refresh); permissions are looked
 * up on every request so role changes apply immediately (REQ-011.AC5).
 */
@Service
public class JwtService {

    public enum TokenType { ACCESS, REFRESH }

    private static final long ACCESS_TOKEN_MILLIS = 1000L * 60 * 30;            // 30 minutes
    private static final long REFRESH_TOKEN_MILLIS = 1000L * 60 * 60 * 24 * 7;  // 7 days
    private static final String TYPE_CLAIM = "type";

    private final Key key;

    public JwtService(@Value("${jwt.secret.key}") String secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String issue(User user, TokenType type) {
        long now = System.currentTimeMillis();
        long ttl = type == TokenType.ACCESS ? ACCESS_TOKEN_MILLIS : REFRESH_TOKEN_MILLIS;
        return Jwts.builder()
                .setClaims(Map.of(TYPE_CLAIM, type.name()))
                .setSubject(user.getEmail())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttl))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** The email in a valid, unexpired token of the given type; empty for anything else. */
    public Optional<String> subject(String token, TokenType expected) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
            if (!expected.name().equals(claims.get(TYPE_CLAIM, String.class))) {
                return Optional.empty();
            }
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
