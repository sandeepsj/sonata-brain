package com.example.sonata_brain.common.docs;

import com.example.sonata_brain.access.Routes;
import com.example.sonata_brain.common.error.ApiError;
import com.example.sonata_brain.common.logging.RequestLoggingFilter;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The generated API spec (REQ-009): served at /v3/api-docs, browsable at /swagger-ui.html, committed as
 * docs/api/openapi.json. Adds what springdoc can't see in the controllers: the bearer token and which route each
 * operation needs, the request/session id headers, and the standard error body.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";
    private static final String ERROR_REF = "#/components/schemas/ApiError";
    /** Endpoints that work without an access token (REQ-011.AC11). */
    private static final Set<String> OPEN = Set.of("POST /api/auth/login", "POST /api/auth/refresh",
            "POST /api/users/signup", "GET /heartbeat");
    private static final Map<String, String> ERRORS = Map.of(
            "400", "Invalid request; `details` lists field problems",
            "401", "Missing, invalid or expired access token",
            "403", "The token's routes don't allow this call",
            "404", "Not found",
            "409", "Conflict, e.g. a duplicate",
            "500", "Unexpected error; quote the X-Request-Id when reporting it");

    @Bean
    public OpenAPI sonataOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Sonata API").version("self-service").description("""
                        Backend API of Sonata.

                        **Auth.** `POST /api/auth/login` returns an access token (30 min), a refresh token (7 days) and \
                        the user's `routes`. Send `Authorization: Bearer <accessToken>`. On 401, call \
                        `POST /api/auth/refresh` with `Authorization: Bearer <refreshToken>` once, then log in again.

                        **Routes.** Access is granted per path group, e.g. `{"path": "/api/payments/**", "read": true, \
                        "write": false}`. GET needs `read`; POST, PUT, PATCH and DELETE need `write`. Each operation \
                        below says which route it needs. A missing flag gives 403. Role changes reach the token at the \
                        next refresh; `GET /api/users/me` shows them at once.

                        **Request and session ids.** Every response has `X-Request-Id`; show it in error messages. \
                        Send `X-Session-Id` (one id per browser tab) so a tab's requests can be traced together.

                        **Errors.** Every documented error uses the `ApiError` body. Some older endpoints still answer \
                        404/400 with an empty body."""))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("The accessToken from login or refresh")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    @Bean
    public OpenApiCustomizer sonataOperations() {
        return openApi -> {
            ModelConverters.getInstance().readAll(ApiError.class).forEach(openApi.getComponents()::addSchemas);
            openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, op) -> {
                boolean open = OPEN.contains(method.name() + " " + path);
                addIdHeaders(op);
                if (open) {
                    op.setSecurity(List.of());
                } else if (!path.equals("/api/users/me")) {
                    describeRoute(op, method, path);
                }
                addErrors(op, open);
            }));
        };
    }

    private static void addIdHeaders(Operation op) {
        List<io.swagger.v3.oas.models.parameters.Parameter> params =
                op.getParameters() == null ? new ArrayList<>() : new ArrayList<>(op.getParameters());
        params.add(new HeaderParameter().name(RequestLoggingFilter.REQUEST_ID_HEADER).required(false)
                .schema(new StringSchema().pattern("^[A-Za-z0-9-]{1,64}$"))
                .description("Optional id for this request; echoed back. A new one is made when missing or invalid."));
        params.add(new HeaderParameter().name(RequestLoggingFilter.SESSION_ID_HEADER).required(false)
                .schema(new StringSchema().maxLength(128))
                .description("Optional client session id, e.g. one per browser tab. Only used for logging."));
        op.setParameters(params);
    }

    private static void describeRoute(Operation op, PathItem.HttpMethod method, String path) {
        Routes.CATALOGUE.stream()
                .filter(route -> path.equals(route.substring(0, route.length() - 3)) || path.startsWith(route.substring(0, route.length() - 2)))
                .findFirst()
                .ifPresent(route -> {
                    String need = method == PathItem.HttpMethod.GET || method == PathItem.HttpMethod.HEAD ? "read" : "write";
                    String line = "Needs **" + need + "** on route `" + route + "`.";
                    op.setDescription(op.getDescription() == null ? line : line + "\n\n" + op.getDescription());
                });
    }

    private static void addErrors(Operation op, boolean open) {
        ApiResponses responses = op.getResponses() == null ? new ApiResponses() : op.getResponses();
        ERRORS.forEach((code, text) -> {
            if (open && (code.equals("401") || code.equals("403"))) {
                return;
            }
            ApiResponse existing = responses.get(code);
            ApiResponse response = existing != null ? existing : new ApiResponse().description(text);
            response.setContent(new Content().addMediaType("application/json",
                    new MediaType().schema(new Schema<ApiError>().$ref(ERROR_REF))));
            responses.addApiResponse(code, response);
        });
        op.setResponses(responses);
    }
}
