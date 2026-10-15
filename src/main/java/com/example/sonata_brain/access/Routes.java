package com.example.sonata_brain.access;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The fixed catalogue of paths that roles grant access to, and helpers over it (REQ-011, REQ-012). */
public final class Routes {

    public static final List<String> CATALOGUE = List.of(
            "/api/students/**",
            "/api/student-sessions/**",
            "/api/courses/**",
            "/api/fee-structures/**",
            "/api/payments/**",
            "/api/discounts/**",
            "/api/dues/**",
            "/api/users/**",
            "/api/roles/**",
            "/api/logging/**");

    private Routes() {
    }

    public static String readAuthority(String path) {
        return "READ:" + path;
    }

    public static String writeAuthority(String path) {
        return "WRITE:" + path;
    }

    /** Spring authorities such as READ:/api/students/** for the given routes. */
    public static List<GrantedAuthority> authorities(Collection<RouteAccess> routes) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (RouteAccess route : routes) {
            if (route.read()) {
                authorities.add(new SimpleGrantedAuthority(readAuthority(route.path())));
            }
            if (route.write()) {
                authorities.add(new SimpleGrantedAuthority(writeAuthority(route.path())));
            }
        }
        return authorities;
    }

    /** Per path, read if any grant reads and write if any grant writes (REQ-012.AC10). Catalogue order, granted only. */
    public static List<RouteAccess> union(Collection<RouteAccess> grants) {
        Map<String, RouteAccess> merged = new LinkedHashMap<>();
        for (RouteAccess g : grants) {
            merged.merge(g.path(), g, (a, b) -> new RouteAccess(a.path(), a.read() || b.read(), a.write() || b.write()));
        }
        return CATALOGUE.stream()
                .map(merged::get)
                .filter(r -> r != null && r.grantsAnything())
                .toList();
    }

    /** Every catalogue path, with false flags where nothing is granted (REQ-012.AC8). */
    public static List<RouteAccess> full(Collection<RouteAccess> grants) {
        List<RouteAccess> granted = union(grants);
        return CATALOGUE.stream()
                .map(path -> granted.stream().filter(r -> r.path().equals(path)).findFirst()
                        .orElse(new RouteAccess(path, false, false)))
                .toList();
    }
}
