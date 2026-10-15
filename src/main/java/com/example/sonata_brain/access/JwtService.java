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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Issues and checks JWTs (REQ-016). Access tokens carry the user's email and effective routes, so requests are
 * authorised from the token alone (REQ-011.AC9). Refresh tokens carry only the email.
 */
@Service
public class JwtService {

    public enum TokenType { ACCESS, REFRESH }

    /** What a valid access token says about its holder. */
    public record AccessClaims(String email, List<RouteAccess> routes) {
    }

    private static final long ACCESS_TOKEN_MILLIS = 1000L * 60 * 30;            // 30 minutes
    private static final long REFRESH_TOKEN_MILLIS = 1000L * 60 * 60 * 24 * 7;  // 7 days
    private static final String TYPE_CLAIM = "type";
    private static final String ROUTES_CLAIM = "routes";

    private final Key key;

    public JwtService(@Value("${jwt.secret.key}") String secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String issueAccess(String email, List<RouteAccess> routes) {
        List<Map<String, Object>> claim = routes.stream()
                .map(r -> Map.<String, Object>of("path", r.path(), "read", r.read(), "write", r.write()))
                .toList();
        Map<String, Object> claims = new HashMap<>();
        claims.put(TYPE_CLAIM, TokenType.ACCESS.name());
        claims.put(ROUTES_CLAIM, claim);
        return build(email, claims, ACCESS_TOKEN_MILLIS);
    }

    public String issueRefresh(String email) {
        return build(email, Map.of(TYPE_CLAIM, TokenType.REFRESH.name()), REFRESH_TOKEN_MILLIS);
    }

    /** The holder of a valid, unexpired access token; empty for anything else. */
    public Optional<AccessClaims> verifyAccess(String token) {
        return parse(token, TokenType.ACCESS).map(claims -> new AccessClaims(claims.getSubject(), routes(claims)));
    }

    /** The email in a valid, unexpired refresh token; empty for anything else. */
    public Optional<String> verifyRefresh(String token) {
        return parse(token, TokenType.REFRESH).map(Claims::getSubject);
    }

    private String build(String email, Map<String, Object> claims, long ttl) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttl))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private Optional<Claims> parse(String token, TokenType expected) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
            if (!expected.name().equals(claims.get(TYPE_CLAIM, String.class)) || claims.getSubject() == null) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static List<RouteAccess> routes(Claims claims) {
        if (!(claims.get(ROUTES_CLAIM) instanceof List<?> raw)) {
            return List.of();
        }
        return raw.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(m -> new RouteAccess(String.valueOf(m.get("path")),
                        Boolean.TRUE.equals(m.get("read")), Boolean.TRUE.equals(m.get("write"))))
                .toList();
    }
}
