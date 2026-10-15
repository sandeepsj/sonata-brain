package com.example.sonata_brain.access;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A named list of route grants (REQ-012). */
@Entity
@Table(name = "role")
public class Role {

    public static final String ADMIN = "ADMIN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "built_in", nullable = false)
    private boolean builtIn;

    // Only routes with at least one flag are stored.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_route", joinColumns = @JoinColumn(name = "role_id"))
    private Set<RouteAccess> routes = new HashSet<>();

    protected Role() {
    }

    public Role(String name, String description, Collection<RouteAccess> routes) {
        this.name = name;
        this.description = description;
        setRoutes(routes);
    }

    /** Every catalogue path, false where nothing is granted. */
    public List<RouteAccess> fullRoutes() {
        return Routes.full(routes);
    }

    public void setRoutes(Collection<RouteAccess> granted) {
        routes.clear();
        granted.stream().filter(RouteAccess::grantsAnything).forEach(routes::add);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public Set<RouteAccess> getRoutes() {
        return routes;
    }
}
