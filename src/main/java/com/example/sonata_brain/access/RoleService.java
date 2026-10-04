package com.example.sonata_brain.access;

import com.example.sonata_brain.access.AccessDtos.RoleRequest;
import com.example.sonata_brain.access.AccessDtos.RoleResponse;
import com.example.sonata_brain.common.error.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
        Role role = new Role(name, request.description(), parsePermissions(request.permissions()));
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
        role.setPermissions(parsePermissions(request.permissions()));
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

    private static Map<Resource, Access> parsePermissions(Map<String, String> raw) {
        Map<Resource, Access> parsed = new EnumMap<>(Resource.class);
        if (raw == null) {
            return parsed;
        }
        raw.forEach((resourceName, accessName) -> {
            Resource resource;
            Access access;
            try {
                resource = Resource.valueOf(resourceName);
            } catch (IllegalArgumentException e) {
                throw ApiException.invalidField("permissions." + resourceName, "Unknown resource '" + resourceName + "'");
            }
            try {
                access = Access.valueOf(String.valueOf(accessName));
            } catch (IllegalArgumentException e) {
                throw ApiException.invalidField("permissions." + resourceName,
                        "Access must be READ, WRITE or NONE, not '" + accessName + "'");
            }
            parsed.put(resource, access);
        });
        return parsed;
    }
}
