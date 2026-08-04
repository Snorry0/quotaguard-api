package com.snor.quotaguard.config;

import com.snor.quotaguard.dto.response.ErrorResponse;
import com.snor.quotaguard.dto.response.FieldErrorDetail;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Builds the OpenAPI 3.0 document for the QuotaGuard API.
 *
 * <p>The bean declares the API {@link Info} (markdown description with the
 * authentication flow and links), {@link ExternalDocumentation}, the dev and
 * production servers, the {@code bearerAuth} HTTP bearer security scheme, a
 * global {@link SecurityRequirement}, the eight tag descriptions, seven reusable
 * error {@link ApiResponse}s (BadRequest, Unauthorized, Forbidden, NotFound,
 * Conflict, TooManyRequests, InternalServerError), three reusable
 * {@link Parameter}s (Page, Size, Sort) and the {@link ErrorResponse} /
 * {@link FieldErrorDetail} schemas (generated from the records by
 * swagger-core's {@link ModelConverters}).</p>
 *
 * <p><strong>Operation ID contract</strong> (explicit, stable — applied
 * consistently by the controller layer, Phase 2):</p>
 * <pre>
 * register               login
 * getCurrentUser         listUsers             getUser              createUser
 * updateUser             deleteUser
 * startSession           endSession            getActiveSession     getSessionHistory
 * consumeUsage           getUsageHistory
 * getCurrentQuota        resetAllQuotas
 * getCurrentPenalties
 * getUsageStats          getUsageTrend
 * getAuditEvents         getAuditEvent
 * </pre>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";
    private static final String ERROR_SCHEMA_REF = "#/components/schemas/ErrorResponse";

    private static final String INFO_DESCRIPTION = """
            Domain-neutral backend system for adaptive quota enforcement, progressive penalties,
            usage tracking, session regulation, and behavioral analytics.

            ## Authentication

            Most endpoints require a JWT bearer token. Obtain one by calling `POST /api/v1/auth/login`
            with your credentials; the response contains a short-lived JWT `access_token`
            (~15 minutes by default, configurable via `JWT_EXPIRATION_MINUTES`) and a long-lived
            opaque `refresh_token` (~30 days by default, configurable via `JWT_REFRESH_EXPIRATION_DAYS`).
            Use the refresh token at `POST /api/v1/auth/refresh` to obtain a new pair (the old
            refresh token is revoked — rotation); revoke it at `POST /api/v1/auth/logout`.

            Include it on every request as:

            ```
            Authorization: Bearer <access_token>
            ```

            Token claims: `sub` = user id, `role` = user role (`USER` or `ADMIN`). Tokens are bound to
            the user id rather than the email, so changing your email does not invalidate active tokens.

            ## Interactive documentation

            Full interactive documentation is available at [the Swagger UI](/swagger-ui.html).

            ## References

            * [Validation reference](docs/08-validation.md) — custom validation annotations, configuration keys, and the error contract.
            * [API guidelines](docs/07-api-guidelines.md) — conventions for designing and maintaining the API.

            ## Error handling

            Validation failures return `400 Bad Request`; state conflicts return `409 Conflict`; quota
            exceeded and active-penalty failures return `429 Too Many Requests`. There is no `422`:
            structural problems are always `400`.

            ## Roles

            Admin-only endpoints require the `ADMIN` role, enforced by `@PreAuthorize` and noted in each
            operation's description.
            """;

    private static final String EX_VALIDATION_ERROR = """
            {
              "timestamp": "2026-08-02T10:15:30.123456Z",
              "status": 400,
              "error": "Bad Request",
              "message": "Validation failed",
              "path": "/api/v1/auth/register",
              "validationErrors": {
                "email": "Email must be provided in normalized form: trimmed, lowercase and a well-formed address",
                "password": "Password must be between the configured minimum and maximum length and must satisfy all configured character-class requirements"
              },
              "errors": [
                {
                  "field": "email",
                  "rejectedValue": "Demo@Example.COM",
                  "message": "Email must be provided in normalized form: trimmed, lowercase and a well-formed address"
                },
                {
                  "field": "password",
                  "rejectedValue": "password",
                  "message": "Password must be between the configured minimum and maximum length and must satisfy all configured character-class requirements"
                }
              ]
            }
            """;

    private static final String EX_CONSTRAINT_VIOLATION = """
            {
              "timestamp": "2026-08-02T10:16:00.000000Z",
              "status": 400,
              "error": "Bad Request",
              "message": "Validation failed",
              "path": "/api/v1/usage/history",
              "validationErrors": {
                "page": "must be greater than or equal to 0"
              },
              "errors": [
                {
                  "field": "page",
                  "rejectedValue": -1,
                  "message": "must be greater than or equal to 0"
                }
              ]
            }
            """;

    private static final String EX_MISSING_TOKEN = """
            {
              "timestamp": "2026-08-02T10:16:30.000000Z",
              "status": 401,
              "error": "Unauthorized",
              "message": "Authentication is required to access this resource",
              "path": "/api/v1/quota",
              "validationErrors": null,
              "errors": null
            }
            """;

    private static final String EX_INVALID_CREDENTIALS = """
            {
              "timestamp": "2026-08-02T10:16:45.000000Z",
              "status": 401,
              "error": "Unauthorized",
              "message": "Invalid email or password",
              "path": "/api/v1/auth/login",
              "validationErrors": null,
              "errors": null
            }
            """;

    private static final String EX_INVALID_REFRESH_TOKEN = """
            {
              "timestamp": "2026-08-03T10:15:30Z",
              "status": 401,
              "error": "Unauthorized",
              "message": "Invalid or expired refresh token",
              "path": "/api/v1/auth/refresh",
              "validationErrors": null,
              "errors": null
            }
            """;

    private static final String EX_INSUFFICIENT_ROLE = """
            {
              "timestamp": "2026-08-02T10:17:00.000000Z",
              "status": 403,
              "error": "Forbidden",
              "message": "You do not have permission to access this resource",
              "path": "/api/v1/users",
              "validationErrors": null,
              "errors": null
            }
            """;

    private static final String EX_USER_NOT_FOUND = """
            {
              "timestamp": "2026-08-02T10:17:15.000000Z",
              "status": 404,
              "error": "Not Found",
              "message": "User not found",
              "path": "/api/v1/users/8f3c1a90-0000-4000-8000-000000000000",
              "validationErrors": null,
              "errors": null
            }
            """;

    private static final String EX_DUPLICATE_EMAIL = """
            {
              "timestamp": "2026-08-02T10:17:30.000000Z",
              "status": 409,
              "error": "Conflict",
              "message": "An account already exists for email: demo@example.com",
              "path": "/api/v1/users",
              "validationErrors": null,
              "errors": null
            }
            """;

    private static final String EX_SELF_DELETION = """
            {
              "timestamp": "2026-08-02T10:17:45.000000Z",
              "status": 409,
              "error": "Conflict",
              "message": "Admins cannot delete their own account",
              "path": "/api/v1/users/2f07c5b2-4f0d-4090-86c1-021e5f6b80f8",
              "validationErrors": null,
              "errors": null
            }
            """;

    private static final String EX_ACTIVE_SESSION = """
            {
              "timestamp": "2026-08-02T10:18:00.000000Z",
              "status": 409,
              "error": "Conflict",
              "message": "An active usage session already exists",
              "path": "/api/v1/sessions/start",
              "validationErrors": {
                "sessionId": "c9e4d7b2-4f0d-4090-86c1-021e5f6b80f8"
              },
              "errors": null
            }
            """;

    private static final String EX_OPTIMISTIC_LOCK = """
            {
              "timestamp": "2026-08-02T10:18:15.000000Z",
              "status": 409,
              "error": "Conflict",
              "message": "The resource was modified concurrently. Please retry the request.",
              "path": "/api/v1/quota/reset",
              "validationErrors": null,
              "errors": null
            }
            """;

    private static final String EX_INVALID_SESSION_STATE = """
            {
              "timestamp": "2026-08-02T10:18:30.000000Z",
              "status": 409,
              "error": "Conflict",
              "message": "Only active sessions can be ended",
              "path": "/api/v1/sessions/2f07c5b2-4f0d-4090-86c1-021e5f6b80f8/end",
              "validationErrors": null,
              "errors": null
            }
            """;

    private static final String EX_QUOTA_EXCEEDED = """
            {
              "timestamp": "2026-08-02T10:18:45.000000Z",
              "status": 429,
              "error": "Too Many Requests",
              "message": "Daily quota exceeded. Limit=100, used=100, attempted=10",
              "path": "/api/v1/usage/consume",
              "validationErrors": {
                "dailyLimit": "100",
                "usedToday": "100",
                "attemptedAmount": "10",
                "penaltyType": "WARNING"
              },
              "errors": null
            }
            """;

    private static final String EX_PENALTY_ACTIVE = """
            {
              "timestamp": "2026-08-02T10:19:00.000000Z",
              "status": 429,
              "error": "Too Many Requests",
              "message": "Consumption is blocked by active penalty SHORT_COOLDOWN until 2026-08-02T10:34:00",
              "path": "/api/v1/usage/consume",
              "validationErrors": {
                "penaltyType": "SHORT_COOLDOWN",
                "endsAt": "2026-08-02T10:34:00"
              },
              "errors": null
            }
            """;

    private static final String EX_UNEXPECTED = """
            {
              "timestamp": "2026-08-02T10:19:15.000000Z",
              "status": 500,
              "error": "Internal Server Error",
              "message": "Unexpected server error",
              "path": "/api/v1/usage/consume",
              "validationErrors": null,
              "errors": null
            }
            """;

    private final String apiVersion;
    private final String publicUrl;

    public OpenApiConfig(
            @Value("${quotaguard.api-version:0.4.0-SNAPSHOT}") String apiVersion,
            @Value("${QUOTAGUARD_PUBLIC_URL:http://localhost:8080}") String publicUrl
    ) {
        this.apiVersion = apiVersion;
        this.publicUrl = publicUrl;
    }

    @Bean
    public OpenAPI quotaGuardOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("QuotaGuard API")
                        .version(apiVersion)
                        .description(INFO_DESCRIPTION)
                        .contact(new Contact()
                                .name("QuotaGuard API")
                                .url("https://github.com/Snorry0/quotaguard-api"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/license/mit")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project README")
                        .url("https://github.com/Snorry0/quotaguard-api/blob/main/README.md"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development"),
                        new Server()
                                .url(publicUrl)
                                .description("Production (set QUOTAGUARD_PUBLIC_URL)")
                ))
                .addTagsItem(new Tag()
                        .name("Authentication")
                        .description("Registration and login endpoints"))
                .addTagsItem(new Tag()
                        .name("Users")
                        .description("User profile and administration endpoints"))
                .addTagsItem(new Tag()
                        .name("Sessions")
                        .description("Session lifecycle tracking and duration-based quota consumption"))
                .addTagsItem(new Tag()
                        .name("Usage")
                        .description("Resource consumption and usage history endpoints"))
                .addTagsItem(new Tag()
                        .name("Quota")
                        .description("Current quota state and quota reset endpoints"))
                .addTagsItem(new Tag()
                        .name("Penalties")
                        .description("Active and historical penalty state endpoints"))
                .addTagsItem(new Tag()
                        .name("Analytics")
                        .description("Usage statistics, trends, and behavioral insights"))
                .addTagsItem(new Tag()
                        .name("Audit")
                        .description("Read-only audit trail endpoints"))
                .components(components())
                .addSecurityItem(new SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME));
    }

    private Components components() {
        Components components = new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme())
                .addResponses("BadRequest", badRequest())
                .addResponses("Unauthorized", unauthorized())
                .addResponses("Forbidden", forbidden())
                .addResponses("NotFound", notFound())
                .addResponses("Conflict", conflict())
                .addResponses("TooManyRequests", tooManyRequests())
                .addResponses("InternalServerError", internalServerError())
                .addParameters("Page", pageParameter())
                .addParameters("Size", sizeParameter())
                .addParameters("Sort", sortParameter());
        addErrorSchemas(components);
        return components;
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }

    /**
     * Registers the {@link ErrorResponse} and {@link FieldErrorDetail} schemas
     * so the reusable error responses' {@code $ref} to
     * {@code #/components/schemas/ErrorResponse} resolves. The schemas are
     * generated from the record classes by swagger-core, so the shape is
     * documented in one place.
     */
    private void addErrorSchemas(Components components) {
        Map<String, Schema> errorSchemas = ModelConverters.getInstance().read(ErrorResponse.class);
        errorSchemas.forEach(components::addSchemas);
        // The Map-typed `validationErrors` property on ErrorResponse can't carry its
        // value schema via a swagger @Schema annotation (the @Schema.AdditionalPropertiesValue
        // enum triggers a Jackson round-trip failure inside OpenAPIService.build() that makes
        // springdoc strip both ErrorResponse and FieldErrorDetail from the served spec). We
        // post-process here: set additionalProperties to a StringSchema instance (a real Schema,
        // not the enum) so the round-trip clone succeeds and the served spec retains the schemas.
        Schema<?> errorResponse = components.getSchemas().get("ErrorResponse");
        if (errorResponse != null && errorResponse.getProperties() != null) {
            Schema<?> validationErrors = (Schema<?>) errorResponse.getProperties().get("validationErrors");
            if (validationErrors != null) {
                validationErrors.setAdditionalProperties(new StringSchema());
            }
        }
        Map<String, Schema> fieldErrorSchemas = ModelConverters.getInstance().read(FieldErrorDetail.class);
        fieldErrorSchemas.forEach(components::addSchemas);
    }

    private ApiResponse badRequest() {
        return new ApiResponse()
                .description("Validation failed.")
                .content(new Content().addMediaType("application/json", new MediaType()
                        .schema(new Schema<>().$ref(ERROR_SCHEMA_REF))
                        .addExamples("validationError", new Example().value(EX_VALIDATION_ERROR))
                        .addExamples("constraintViolation", new Example().value(EX_CONSTRAINT_VIOLATION))));
    }

    private ApiResponse unauthorized() {
        return new ApiResponse()
                .description("Authentication required or credentials invalid.")
                .content(new Content().addMediaType("application/json", new MediaType()
                        .schema(new Schema<>().$ref(ERROR_SCHEMA_REF))
                        .addExamples("missingToken", new Example().value(EX_MISSING_TOKEN))
                        .addExamples("invalidCredentials", new Example().value(EX_INVALID_CREDENTIALS))
                        .addExamples("invalidRefreshToken", new Example().value(EX_INVALID_REFRESH_TOKEN))));
    }

    private ApiResponse forbidden() {
        return new ApiResponse()
                .description("Authenticated but lacks the required role.")
                .content(new Content().addMediaType("application/json", new MediaType()
                        .schema(new Schema<>().$ref(ERROR_SCHEMA_REF))
                        .addExamples("insufficientRole", new Example().value(EX_INSUFFICIENT_ROLE))));
    }

    private ApiResponse notFound() {
        return new ApiResponse()
                .description("Resource not found.")
                .content(new Content().addMediaType("application/json", new MediaType()
                        .schema(new Schema<>().$ref(ERROR_SCHEMA_REF))
                        .addExamples("userNotFound", new Example().value(EX_USER_NOT_FOUND))));
    }

    private ApiResponse conflict() {
        return new ApiResponse()
                .description("Resource state conflict.")
                .content(new Content().addMediaType("application/json", new MediaType()
                        .schema(new Schema<>().$ref(ERROR_SCHEMA_REF))
                        .addExamples("duplicateEmail", new Example().value(EX_DUPLICATE_EMAIL))
                        .addExamples("selfDeletion", new Example().value(EX_SELF_DELETION))
                        .addExamples("activeSession", new Example().value(EX_ACTIVE_SESSION))
                        .addExamples("optimisticLock", new Example().value(EX_OPTIMISTIC_LOCK))
                        .addExamples("invalidSessionState", new Example().value(EX_INVALID_SESSION_STATE))));
    }

    private ApiResponse tooManyRequests() {
        return new ApiResponse()
                .description("Quota exceeded or penalty active. May include a `Retry-After` header "
                        + "(seconds) when a penalty is active.")
                .content(new Content().addMediaType("application/json", new MediaType()
                        .schema(new Schema<>().$ref(ERROR_SCHEMA_REF))
                        .addExamples("quotaExceeded", new Example().value(EX_QUOTA_EXCEEDED))
                        .addExamples("penaltyActive", new Example().value(EX_PENALTY_ACTIVE))));
    }

    private ApiResponse internalServerError() {
        return new ApiResponse()
                .description("Unexpected server error.")
                .content(new Content().addMediaType("application/json", new MediaType()
                        .schema(new Schema<>().$ref(ERROR_SCHEMA_REF))
                        .addExamples("unexpected", new Example().value(EX_UNEXPECTED))));
    }

    private Parameter pageParameter() {
        return new Parameter()
                .in("query")
                .name("page")
                .description("Zero-based page index.")
                .required(false)
                .schema(new IntegerSchema()
                        .minimum(new BigDecimal("0"))
                        ._default(0));
    }

    private Parameter sizeParameter() {
        return new Parameter()
                .in("query")
                .name("size")
                .description("Page size (1-100).")
                .required(false)
                .schema(new IntegerSchema()
                        .minimum(new BigDecimal("1"))
                        .maximum(new BigDecimal("100"))
                        ._default(20));
    }

    private Parameter sortParameter() {
        return new Parameter()
                .in("query")
                .name("sort")
                .description("Sort field and optional direction in `property[,direction]` format. "
                        + "Allowed properties: `timestamp`, `id`, `action`, `resourceType`, `actorEmail`. "
                        + "Direction defaults to ascending; use `desc` for descending. Example: `timestamp,desc`. "
                        + "Unknown properties return 400. Effective default: `timestamp,id` descending.")
                .required(false)
                .schema(new StringSchema());
    }
}
