package com.example.sonata_brain.access;

import com.example.sonata_brain.access.AccessDtos.LoginRequest;
import com.example.sonata_brain.access.AccessDtos.MeResponse;
import com.example.sonata_brain.access.AccessDtos.RoleRequest;
import com.example.sonata_brain.access.AccessDtos.RoleResponse;
import com.example.sonata_brain.access.AccessDtos.SetRolesRequest;
import com.example.sonata_brain.access.AccessDtos.SignupRequest;
import com.example.sonata_brain.access.AccessDtos.TokenResponse;
import com.example.sonata_brain.access.AccessDtos.UserResponse;
import com.example.sonata_brain.common.error.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** HTTP endpoints for login, users and roles. Thin: validation and rules live in the services. */
public final class AccessControllers {

    private AccessControllers() {
    }

    @RestController
    @RequestMapping("/api/auth")
    public static class AuthController {
        private final AuthService auth;

        public AuthController(AuthService auth) {
            this.auth = auth;
        }

        @PostMapping("/login")
        public TokenResponse login(@RequestBody(required = false) LoginRequest request) {
            return auth.login(request);
        }

        @PostMapping("/refresh")
        public TokenResponse refresh(@RequestHeader(value = "Authorization", required = false) String header) {
            if (header == null || !header.startsWith("Bearer ")) {
                throw ApiException.unauthorized("A refresh token is required");
            }
            return auth.refresh(header.substring("Bearer ".length()));
        }
    }

    @RestController
    @RequestMapping("/api/users")
    public static class UserController {
        private final UserService users;

        public UserController(UserService users) {
            this.users = users;
        }

        @PostMapping("/signup")
        @ResponseStatus(HttpStatus.CREATED)
        public UserResponse signup(@Valid @RequestBody SignupRequest request) {
            return users.signup(request);
        }

        @GetMapping
        public List<UserResponse> list() {
            return users.list();
        }

        @GetMapping("/me")
        public MeResponse me(@AuthenticationPrincipal User me) {
            return MeResponse.of(me);
        }

        @PutMapping("/{id}/roles")
        public UserResponse setRoles(@PathVariable Long id, @Valid @RequestBody SetRolesRequest request) {
            return users.setRoles(id, request.roleIds());
        }
    }

    @RestController
    @RequestMapping("/api/roles")
    public static class RoleController {
        private final RoleService roles;

        public RoleController(RoleService roles) {
            this.roles = roles;
        }

        @GetMapping
        public List<RoleResponse> list(@RequestParam(required = false) String name) {
            return roles.list(name);
        }

        @GetMapping("/{id}")
        public RoleResponse get(@PathVariable Long id) {
            return roles.get(id);
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public RoleResponse create(@RequestBody RoleRequest request) {
            return roles.create(request);
        }

        @PutMapping("/{id}")
        public RoleResponse update(@PathVariable Long id, @RequestBody RoleRequest request) {
            return roles.update(id, request);
        }

        @DeleteMapping("/{id}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void delete(@PathVariable Long id) {
            roles.delete(id);
        }
    }
}
