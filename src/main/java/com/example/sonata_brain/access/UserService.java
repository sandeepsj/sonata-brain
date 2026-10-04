package com.example.sonata_brain.access;

import com.example.sonata_brain.access.AccessDtos.SignupRequest;
import com.example.sonata_brain.access.AccessDtos.UserResponse;
import com.example.sonata_brain.common.error.ApiException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, RoleRepository roles, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    /** Self sign-up: the account starts with no roles and so no access (REQ-014.AC3). */
    public UserResponse signup(SignupRequest request) {
        String email = request.email().trim();
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        return UserResponse.of(users.save(new User(email, passwordEncoder.encode(request.password()))));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return users.findAllByOrderByIdAsc().stream().map(UserResponse::of).toList();
    }

    public UserResponse setRoles(Long userId, List<Long> roleIds) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User " + userId + " not found"));
        Set<Role> newRoles = new HashSet<>();
        for (Long roleId : roleIds) {
            newRoles.add(roles.findById(roleId)
                    .orElseThrow(() -> ApiException.invalidField("roleIds", "Unknown role id " + roleId)));
        }
        boolean losesAdmin = user.hasRole(Role.ADMIN) && newRoles.stream().noneMatch(r -> r.getName().equals(Role.ADMIN));
        if (losesAdmin && users.countByRoles_Name(Role.ADMIN) <= 1) {
            throw ApiException.conflict("This is the last administrator; ADMIN can't be removed");
        }
        user.setRoles(newRoles);
        return UserResponse.of(user);
    }

    /** Creates the account with ADMIN unless an account with that email already exists (REQ-015). */
    public void ensureAdmin(String email, String password) {
        if (users.existsByEmail(email)) {
            return;
        }
        User admin = new User(email, passwordEncoder.encode(password));
        roles.findByName(Role.ADMIN).ifPresent(r -> admin.getRoles().add(r));
        users.save(admin);
    }

    @Override
    @Transactional(readOnly = true)
    public User loadUserByUsername(String email) {
        return users.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));
    }
}
