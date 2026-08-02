package com.snor.quotaguard.user.controller;

import com.snor.quotaguard.user.dto.request.CreateUserRequest;
import com.snor.quotaguard.user.dto.request.UpdateUserRequest;
import com.snor.quotaguard.user.dto.response.UserResponse;
import com.snor.quotaguard.user.service.UserService;
import com.snor.quotaguard.validation.annotation.AllowedPageSize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Users", description = "User profile and administration endpoints")
@Validated
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private static final String USER_EXAMPLE = """
            {
              "id": "2f07c5b2-4f0d-4090-86c1-021e5f6b80f8",
              "email": "demo@example.com",
              "role": "USER",
              "createdAt": "2026-08-02T08:30:00"
            }
            """;

    private final UserService userService;

    @Operation(
            summary = "Get the authenticated user's profile",
            description = "Returns the profile of the currently authenticated user.",
            operationId = "getCurrentUser"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The authenticated user's profile.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(name = "currentUser", summary = "Authenticated user",
                                    value = USER_EXAMPLE))),
            @ApiResponse(responseCode = "401", ref = "Unauthorized")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @Operation(
            summary = "List all users (ADMIN)",
            description = """
                    Returns a page of all users ordered by email. Supports pagination via the
                    `page` and `size` query parameters.
                    Requires the `ADMIN` role.
                    """,
            operationId = "listUsers"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of users.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", ref = "BadRequest"),
            @ApiResponse(responseCode = "401", ref = "Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "Forbidden")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @AllowedPageSize int size
    ) {
        return ResponseEntity.ok(userService.getUsers(page, size));
    }

    @Operation(
            summary = "Get a user by ID (ADMIN)",
            description = "Returns a single user by its UUID identifier. Requires the `ADMIN` role.",
            operationId = "getUser"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The requested user.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(name = "user", summary = "Requested user",
                                    value = USER_EXAMPLE))),
            @ApiResponse(responseCode = "401", ref = "Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "Forbidden"),
            @ApiResponse(responseCode = "404", ref = "NotFound")
    })
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @Operation(
            summary = "Create a new user (ADMIN)",
            description = """
                    Creates a user with the given email, password and optional role, and creates the
                    associated default quota. Publishes a `UserCreatedEvent`.
                    Requires the `ADMIN` role.
                    """,
            operationId = "createUser"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User details to create.",
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = CreateUserRequest.class),
                    examples = @ExampleObject(name = "createUserRequest", summary = "Valid user creation",
                            value = "{\"email\":\"newuser@example.com\",\"password\":\"Password123!\",\"role\":\"USER\"}"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(name = "createdUser", summary = "Created user",
                                    value = USER_EXAMPLE))),
            @ApiResponse(responseCode = "400", ref = "BadRequest"),
            @ApiResponse(responseCode = "401", ref = "Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "Forbidden"),
            @ApiResponse(responseCode = "409", ref = "Conflict")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @Operation(
            summary = "Update a user (ADMIN)",
            description = """
                    Partially updates a user: only provided fields (email, password, role) are
                    changed; omitted fields keep their current value. Publishes a `UserUpdatedEvent`.
                    Requires the `ADMIN` role.
                    """,
            operationId = "updateUser"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Fields to update. Omitted fields are left unchanged.",
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UpdateUserRequest.class),
                    examples = @ExampleObject(name = "updateUserRequest", summary = "Update the email only",
                            value = "{\"email\":\"updated@example.com\"}"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(name = "updatedUser", summary = "Updated user",
                                    value = USER_EXAMPLE))),
            @ApiResponse(responseCode = "400", ref = "BadRequest"),
            @ApiResponse(responseCode = "401", ref = "Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "Forbidden"),
            @ApiResponse(responseCode = "404", ref = "NotFound"),
            @ApiResponse(responseCode = "409", ref = "Conflict")
    })
    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    @Operation(
            summary = "Delete a user (ADMIN)",
            description = """
                    Deletes the user and its quota, then publishes a `UserDeletedEvent`. An admin
                    cannot delete their own account (409).
                    Requires the `ADMIN` role.
                    """,
            operationId = "deleteUser"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted."),
            @ApiResponse(responseCode = "401", ref = "Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "Forbidden"),
            @ApiResponse(responseCode = "409", ref = "Conflict")
    })
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
