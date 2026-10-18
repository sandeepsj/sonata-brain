package com.example.sonata_brain.receipts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** A named receipt template: an HTML document with Handlebars variables, rendered by the frontend (REQ-090). */
@Entity
@Table(name = "receipt_template")
public class ReceiptTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "built_in", nullable = false)
    private boolean builtIn;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(nullable = false, columnDefinition = "text")
    private String html;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    protected ReceiptTemplate() {
    }

    public ReceiptTemplate(String name, String html, String by) {
        this.name = name;
        this.html = html;
        this.createdAt = LocalDateTime.now();
        this.createdBy = by;
        this.updatedAt = createdAt;
        this.updatedBy = by;
    }

    public void replace(String name, String html, String by) {
        this.name = name;
        this.html = html;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = by;
    }

    public void makeDefault() {
        this.isDefault = true;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public String getHtml() {
        return html;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}
