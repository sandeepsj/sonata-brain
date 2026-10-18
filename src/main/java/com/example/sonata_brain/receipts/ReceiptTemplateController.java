package com.example.sonata_brain.receipts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/** HTTP endpoints for receipt templates (REQ-090, REQ-091). Rules live in {@link ReceiptTemplateService}. */
@RestController
@RequestMapping("/api/receipt-templates")
public class ReceiptTemplateController {

    /** {@code html} is read as JSON so a value that isn't a string is reported as a field problem (REQ-090.AC7). */
    public record TemplateRequest(String name, JsonNode html) {
    }

    /** A list item: everything but the HTML (REQ-090.AC1). */
    public record TemplateSummary(Long id, String name, boolean builtIn, @JsonProperty("isDefault") boolean isDefault,
                                  LocalDateTime createdAt, String createdBy, LocalDateTime updatedAt,
                                  String updatedBy) {
        static TemplateSummary of(ReceiptTemplate t) {
            return new TemplateSummary(t.getId(), t.getName(), t.isBuiltIn(), t.isDefault(), t.getCreatedAt(),
                    t.getCreatedBy(), t.getUpdatedAt(), t.getUpdatedBy());
        }
    }

    public record TemplateResponse(Long id, String name, boolean builtIn,
                                   @JsonProperty("isDefault") boolean isDefault, String html,
                                   LocalDateTime createdAt, String createdBy, LocalDateTime updatedAt,
                                   String updatedBy) {
        static TemplateResponse of(ReceiptTemplate t) {
            return new TemplateResponse(t.getId(), t.getName(), t.isBuiltIn(), t.isDefault(), t.getHtml(),
                    t.getCreatedAt(), t.getCreatedBy(), t.getUpdatedAt(), t.getUpdatedBy());
        }
    }

    private final ReceiptTemplateService templates;

    public ReceiptTemplateController(ReceiptTemplateService templates) {
        this.templates = templates;
    }

    @GetMapping
    public List<TemplateSummary> list() {
        return templates.list();
    }

    @GetMapping("/default")
    public TemplateResponse getDefault() {
        return templates.getDefault();
    }

    @GetMapping("/{id}")
    public TemplateResponse get(@PathVariable Long id) {
        return templates.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateResponse create(@RequestBody TemplateRequest request) {
        return templates.create(request);
    }

    @PutMapping("/{id}")
    public TemplateResponse update(@PathVariable Long id, @RequestBody TemplateRequest request) {
        return templates.update(id, request);
    }

    @PatchMapping("/{id}/default")
    public TemplateResponse makeDefault(@PathVariable Long id) {
        return templates.makeDefault(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        templates.delete(id);
    }
}
