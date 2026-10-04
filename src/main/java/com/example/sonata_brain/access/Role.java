package com.example.sonata_brain.access;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** A named collection of resource permissions (REQ-012). */
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permission", joinColumns = @JoinColumn(name = "role_id"))
    @MapKeyColumn(name = "resource")
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "access", nullable = false)
    @Enumerated(EnumType.STRING)
    private Map<Resource, Access> permissions = new HashMap<>();

    protected Role() {
    }

    public Role(String name, String description, Map<Resource, Access> permissions) {
        this.name = name;
        this.description = description;
        setPermissions(permissions);
    }

    /** Every resource, NONE where nothing is granted. */
    public Map<Resource, Access> fullPermissions() {
        Map<Resource, Access> all = new EnumMap<>(Resource.class);
        for (Resource r : Resource.values()) {
            all.put(r, permissions.getOrDefault(r, Access.NONE));
        }
        return all;
    }

    public void setPermissions(Map<Resource, Access> granted) {
        permissions.clear();
        granted.forEach((resource, access) -> {
            if (access != Access.NONE) {
                permissions.put(resource, access);
            }
        });
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

    public Map<Resource, Access> getPermissions() {
        return permissions;
    }
}
