package com.example.sonata_brain.common.security;

import com.example.sonata_brain.access.Resource;

import java.util.List;

/** Which resource each API path belongs to (REQ-011). GET needs READ; every other method needs WRITE. */
public final class ResourceRoutes {

    public record Route(String pattern, Resource resource) {
    }

    public static final List<Route> ROUTES = List.of(
            new Route("/api/students/**", Resource.STUDENT),
            new Route("/api/student-sessions/**", Resource.SESSION),
            new Route("/api/payments/**", Resource.PAYMENT),
            new Route("/api/discounts/**", Resource.PAYMENT),
            new Route("/api/courses/**", Resource.COURSE),
            new Route("/api/fee-structures/**", Resource.FEE_STRUCTURE),
            new Route("/api/fee-timeline/**", Resource.FEE_STRUCTURE),
            new Route("/api/dues/**", Resource.DUES),
            new Route("/api/users/**", Resource.USER),
            new Route("/api/roles/**", Resource.USER),
            new Route("/api/logging/**", Resource.USER));

    private ResourceRoutes() {
    }
}
