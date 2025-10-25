package com.example.sonata_brain.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "account")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    private Scope scope = Scope.NONE;

    @Column(name = "password", nullable = false)
    private String password;

    public User() {
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // This is what links your "scope" to Spring's permissions
        return List.of(new SimpleGrantedAuthority(scope.name()));
    }

    @Override
    public String getUsername() {
        // Spring calls this "username," but for us, it's the email.
        return this.email;
    }

    public long getId(){
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public Scope getScope(){
        return scope;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public String getPassword() { return password; }
}
