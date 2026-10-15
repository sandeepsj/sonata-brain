package com.example.sonata_brain.access;

import com.example.sonata_brain.access.AccessDtos.RoleRequest;
import com.example.sonata_brain.access.AccessDtos.RoleResponse;
import com.example.sonata_brain.access.AccessDtos.RouteRequest;
import com.example.sonata_brain.common.error.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class RoleService {

    private final RoleRepository roles;

    public RoleService(RoleRepository roles) {
        this.roles = roles;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> list(String name) {
        List<Role> found = name == null
                ? roles.findAllByOrderByNameAsc()
                : roles.findByName(name).map(List::of).orElse(List.of());
        return found.stream().map(RoleResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse get(Long id) {
        return RoleResponse.of(find(id));
    }

    public RoleResponse create(RoleRequest request) {
        String name = validName(request);
        if (roles.existsByName(name)) {
            throw ApiException.conflict("A role named '" + name + "' already exists");
        }
        Role role = new Role(name, request.description(), parseRoutes(request.routes()));
        return RoleResponse.of(roles.save(role));
    }

    public RoleResponse update(Long id, RoleRequest request) {
        Role role = find(id);
        String name = validName(request);
        if (roles.existsByNameAndIdNot(name, id)) {
            throw ApiException.conflict("A role named '" + name + "' already exists");
        }
        role.setName(name);
        role.setDescription(request.description());
        role.setRoutes(parseRoutes(request.routes()));
        return RoleResponse.of(role);
    }

    /** Removing a role also removes it from every user (database cascade). The built-in ADMIN role stays. */
    public void delete(Long id) {
        Role role = find(id);
        if (role.isBuiltIn()) {
            throw ApiException.conflict("The built-in role '" + role.getName() + "' can't be deleted");
        }
        roles.delete(role);
    }

    private Role find(Long id) {
        return roles.findById(id).orElseThrow(() -> ApiException.notFound("Role " + id + " not found"));
    }

    private static String validName(RoleRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw ApiException.invalidField("name", "Role name is required");
        }
        return request.name().trim();
    }

    private static List<RouteAccess> parseRoutes(List<RouteRequest> raw) {
        if (raw == null) {
            return List.of();
        }
        Set<String> seen = new HashSet<>();
        List<RouteAccess> parsed = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            RouteRequest route = raw.get(i);
            String field = "routes[" + i + "].path";
            if (route == null || route.path() == null || !Routes.CATALOGUE.contains(route.path())) {
                throw ApiException.invalidField(field, "Unknown route '" + (route == null ? null : route.path())
                        + "'; allowed: " + String.join(", ", Routes.CATALOGUE));
            }
            if (!seen.add(route.path())) {
                throw ApiException.invalidField(field, "Route '" + route.path() + "' is listed more than once");
            }
            parsed.add(new RouteAccess(route.path(), Boolean.TRUE.equals(route.read()),
                    Boolean.TRUE.equals(route.write())));
        }
        return parsed;
    }
}
