package com.example.sonata_brain.receipts;

import com.example.sonata_brain.common.context.RequestContext;
import com.example.sonata_brain.common.error.ApiError.FieldProblem;
import com.example.sonata_brain.common.error.ApiException;
import com.example.sonata_brain.receipts.ReceiptTemplateController.TemplateRequest;
import com.example.sonata_brain.receipts.ReceiptTemplateController.TemplateResponse;
import com.example.sonata_brain.receipts.ReceiptTemplateController.TemplateSummary;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Receipt templates (REQ-090) and the institute-wide default (REQ-091). */
@Service
@Transactional
public class ReceiptTemplateService {

    static final int MAX_NAME = 80;
    static final int MAX_HTML_BYTES = 1024 * 1024;

    private final ReceiptTemplateRepository templates;

    public ReceiptTemplateService(ReceiptTemplateRepository templates) {
        this.templates = templates;
    }

    @Transactional(readOnly = true)
    public List<TemplateSummary> list() {
        return templates.findAllInOrder().stream().map(TemplateSummary::of).toList();
    }

    @Transactional(readOnly = true)
    public TemplateResponse get(Long id) {
        return TemplateResponse.of(find(id));
    }

    @Transactional(readOnly = true)
    public TemplateResponse getDefault() {
        return TemplateResponse.of(templates.findDefault()
                .orElseThrow(() -> ApiException.notFound("There is no default receipt template")));
    }

    public TemplateResponse create(TemplateRequest request) {
        String name = validate(request, null);
        return TemplateResponse.of(templates.save(new ReceiptTemplate(name, request.html().asText(), user())));
    }

    public TemplateResponse update(Long id, TemplateRequest request) {
        ReceiptTemplate template = find(id);
        if (template.isBuiltIn()) {
            throw ApiException.conflict("The built-in template '" + template.getName()
                    + "' can't be changed; save your changes as a new template");
        }
        String name = validate(request, id);
        template.replace(name, request.html().asText(), user());
        return TemplateResponse.of(templates.saveAndFlush(template));
    }

    /** REQ-090.AC5: deleting the default hands the default back to the built-in template. */
    public void delete(Long id) {
        ReceiptTemplate template = find(id);
        if (template.isBuiltIn()) {
            throw ApiException.conflict("The built-in template '" + template.getName() + "' can't be deleted");
        }
        boolean wasDefault = template.isDefault();
        templates.delete(template);
        templates.flush();
        if (wasDefault) {
            templates.makeBuiltInDefault();
        }
    }

    /** REQ-091.AC2: the previous default loses the flag in the same transaction. */
    public TemplateResponse makeDefault(Long id) {
        ReceiptTemplate template = find(id);
        templates.clearDefaultExcept(id);
        template.makeDefault();
        return TemplateResponse.of(templates.saveAndFlush(template));
    }

    private ReceiptTemplate find(Long id) {
        return templates.findById(id)
                .orElseThrow(() -> ApiException.notFound("Receipt template " + id + " not found"));
    }

    /** Returns the trimmed name. Name and html problems are reported together; a taken name is a 409. */
    private String validate(TemplateRequest request, Long id) {
        String raw = request == null ? null : request.name();
        String name = raw == null ? "" : raw.trim();
        List<FieldProblem> problems = new ArrayList<>();
        if (name.isEmpty()) {
            problems.add(new FieldProblem("name", "Name is required"));
        } else if (name.length() > MAX_NAME) {
            problems.add(new FieldProblem("name", "Name must be at most " + MAX_NAME + " characters"));
        }
        htmlProblem(request == null ? null : request.html()).ifPresent(m -> problems.add(new FieldProblem("html", m)));
        if (!problems.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The receipt template is not valid", problems);
        }
        if (templates.nameTaken(name, id)) {
            throw ApiException.conflict("A receipt template named '" + name + "' already exists");
        }
        return name;
    }

    /** REQ-090.AC7: only presence, type and size are checked; any markup, CSS and scripts are accepted. */
    private static Optional<String> htmlProblem(JsonNode html) {
        if (html == null || html.isNull() || html.isMissingNode()) {
            return Optional.of("html is required");
        }
        if (!html.isTextual()) {
            return Optional.of("html must be a string");
        }
        if (html.asText().isBlank()) {
            return Optional.of("html must not be blank");
        }
        if (html.asText().getBytes(StandardCharsets.UTF_8).length > MAX_HTML_BYTES) {
            return Optional.of("html must be at most 1 MB");
        }
        return Optional.empty();
    }

    private static String user() {
        return RequestContext.current().map(RequestContext.Values::user).orElse(null);
    }
}
