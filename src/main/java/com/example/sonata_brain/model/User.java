package com.example.sonata_brain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "account")
public class User {
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
