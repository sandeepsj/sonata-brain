package com.example.sonata_brain.access;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/** A login account. Its permissions are the union of its roles' permissions (REQ-012.AC5). */
@Entity
@Table(name = "account")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    protected User() {
    }

    public User(String email, String passwordHash) {
        this.email = email;
        this.password = passwordHash;
    }

    /** Strongest access per resource across all roles. */
    public Map<Resource, Access> effectiveAccess() {
        Map<Resource, Access> access = new EnumMap<>(Resource.class);
        for (Role role : roles) {
            role.getPermissions().forEach((resource, level) -> access.merge(resource, level, Access::max));
        }
        return access;
    }

    /** Authorities such as PAYMENT_READ, PAYMENT_WRITE. WRITE also grants READ (REQ-011.AC2). */
    public SortedSet<String> permissionNames() {
        SortedSet<String> names = new TreeSet<>();
        effectiveAccess().forEach((resource, level) -> {
            if (level == Access.READ || level == Access.WRITE) {
                names.add(resource.readAuthority());
            }
            if (level == Access.WRITE) {
                names.add(resource.writeAuthority());
            }
        });
        return names;
    }

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(r -> r.getName().equals(roleName));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = permissionNames().stream()
                .map(name -> (GrantedAuthority) new SimpleGrantedAuthority(name))
                .toList();
        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}
