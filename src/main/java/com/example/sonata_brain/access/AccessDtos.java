package com.example.sonata_brain.access;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Request and response bodies of the access API. */
public final class AccessDtos {

    private AccessDtos() {
    }

    public record LoginRequest(String email, String password) {
    }

    public record TokenResponse(String accessToken, String refreshToken, List<RouteAccess> routes) {
    }

    public record SignupRequest(@NotBlank(message = "Email is required") String email,
                                @NotBlank(message = "Password is required") String password) {
    }

    /** A missing flag means false. Paths are checked against the catalogue (REQ-012.AC7). */
    public record RouteRequest(String path, Boolean read, Boolean write) {
    }

    public record RoleRequest(String name, String description, List<RouteRequest> routes) {
    }

    public record RoleResponse(Long id, String name, String description, boolean builtIn, List<RouteAccess> routes) {
        static RoleResponse of(Role role) {
            return new RoleResponse(role.getId(), role.getName(), role.getDescription(), role.isBuiltIn(),
                    role.fullRoutes());
        }
    }

    public record RoleRef(Long id, String name) {
        static List<RoleRef> of(Collection<Role> roles) {
            return roles.stream()
                    .sorted(Comparator.comparing(Role::getName))
                    .map(r -> new RoleRef(r.getId(), r.getName()))
                    .toList();
        }
    }

    public record UserResponse(Long id, String email, List<RoleRef> roles) {
        static UserResponse of(User user) {
            return new UserResponse(user.getId(), user.getEmail(), RoleRef.of(user.getRoles()));
        }
    }

    public record MeResponse(Long id, String email, List<RoleRef> roles, List<RouteAccess> routes) {
        static MeResponse of(User user) {
            return new MeResponse(user.getId(), user.getEmail(), RoleRef.of(user.getRoles()), user.effectiveRoutes());
        }
    }

    public record SetRolesRequest(@NotNull(message = "roleIds is required") List<Long> roleIds) {
    }
}
