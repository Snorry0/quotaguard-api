package com.snor.quotaguard.auth.controller;

import com.snor.quotaguard.auth.dto.request.LoginRequest;
import com.snor.quotaguard.auth.dto.request.RegisterRequest;
import com.snor.quotaguard.auth.dto.response.AuthResponse;
import com.snor.quotaguard.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Authentication",
        description = "Registration and login endpoints"
)
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String AUTH_RESPONSE_EXAMPLE = """
            {
              "access_token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyZjA3YzViMi00ZjBkLTQwOTAtODZjMS0wMjFlNWY2YjgwZjgiLCJyb2xlIjoiVVNFUiIsImlhdCI6MTcyMjUwMDAwMCwiZXhwIjoxNzIyNTAzNjAwfQ.example-signature",
              "token_type": "Bearer",
              "expires_at": "2026-08-02T22:00:00Z",
              "user": {
                "id": "2f07c5b2-4f0d-4090-86c1-021e5f6b80f8",
                "email": "demo@example.com",
                "role": "USER",
                "createdAt": "2026-08-02T08:30:00"
              }
            }
            """;

    private final AuthService authService;

    @Operation(
            summary = "Register a new user account",
            description = """
                    Creates a new `USER` account, publishes a `UserRegisteredEvent`, and returns a
                    bearer token for the new account. The email must be unique and normalized
                    (lowercase, trimmed); the password must satisfy the configured strength policy
                    (min length 8 with upper/lower/digit/special by default).
                    """,
            operationId = "register"
    )
    @SecurityRequirements()
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Registration credentials.",
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = RegisterRequest.class),
                    examples = @ExampleObject(name = "registerRequest", summary = "Valid registration",
                            value = "{\"email\":\"demo@example.com\",\"password\":\"Password123!\"}"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created and authenticated.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(name = "registeredUser",
                                    summary = "Successful registration", value = AUTH_RESPONSE_EXAMPLE))),
            @ApiResponse(responseCode = "400", ref = "BadRequest"),
            @ApiResponse(responseCode = "409", ref = "Conflict")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(
            summary = "Authenticate and obtain a JWT",
            description = """
                    Authenticates with email and password and returns a bearer token valid for
                    12 hours by default (configurable via `JWT_EXPIRATION_HOURS`). Token claims:
                    `sub` = user id, `role`. Tokens are bound to the user id, so changing the
                    email does not invalidate active tokens.
                    """,
            operationId = "login"
    )
    @SecurityRequirements()
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Login credentials.",
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LoginRequest.class),
                    examples = @ExampleObject(name = "loginRequest", summary = "Valid login",
                            value = "{\"email\":\"demo@example.com\",\"password\":\"Password123!\"}"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated successfully.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(name = "authenticatedUser",
                                    summary = "Successful login", value = AUTH_RESPONSE_EXAMPLE))),
            @ApiResponse(responseCode = "400", ref = "BadRequest"),
            @ApiResponse(responseCode = "401", ref = "Unauthorized")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
