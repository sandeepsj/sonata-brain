package com.example.sonata_brain.access;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Request and response bodies of the access API. */
public final class AccessDtos {

    private AccessDtos() {
    }

    public record LoginRequest(String email, String password) {
    }

    public record TokenResponse(String accessToken, String refreshToken, Collection<String> permissions) {
    }

    public record SignupRequest(@NotBlank(message = "Email is required") String email,
                                @NotBlank(message = "Password is required") String password) {
    }

    /** Permissions arrive as raw strings so unknown resources/levels can be reported as 400 (REQ-012.AC1). */
    public record RoleRequest(String name, String description, Map<String, String> permissions) {
    }

    public record RoleResponse(Long id, String name, String description, boolean builtIn,
                               Map<Resource, Access> permissions) {
        static RoleResponse of(Role role) {
            return new RoleResponse(role.getId(), role.getName(), role.getDescription(), role.isBuiltIn(),
                    role.fullPermissions());
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

    public record MeResponse(Long id, String email, List<RoleRef> roles, Collection<String> permissions) {
        static MeResponse of(User user) {
            return new MeResponse(user.getId(), user.getEmail(), RoleRef.of(user.getRoles()), user.permissionNames());
        }
    }

    public record SetRolesRequest(@NotNull(message = "roleIds is required") List<Long> roleIds) {
    }
}
