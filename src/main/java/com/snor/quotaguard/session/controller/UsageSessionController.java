package com.snor.quotaguard.session.controller;

import com.snor.quotaguard.session.dto.request.EndUsageSessionRequest;
import com.snor.quotaguard.session.dto.request.StartUsageSessionRequest;
import com.snor.quotaguard.session.dto.response.EndUsageSessionResponse;
import com.snor.quotaguard.session.dto.response.UsageSessionResponse;
import com.snor.quotaguard.session.service.UsageSessionService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(
        name = "Sessions",
        description = "Session lifecycle tracking and duration-based quota consumption"
)
@Validated
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class UsageSessionController {

    private static final String ACTIVE_SESSION_EXAMPLE = """
            {
              "id": "a1b2c3d4-0000-4000-8000-000000000001",
              "userId": "2f07c5b2-4f0d-4090-86c1-021e5f6b80f8",
              "clientReference": "desktop-client",
              "startedAt": "2026-08-02T09:00:00",
              "endedAt": null,
              "durationSeconds": null,
              "amountConsumed": null,
              "status": "ACTIVE",
              "metadata": null
            }
            """;

    private static final String ENDED_SESSION_EXAMPLE = """
            {
              "session": {
                "id": "a1b2c3d4-0000-4000-8000-000000000001",
                "userId": "2f07c5b2-4f0d-4090-86c1-021e5f6b80f8",
                "clientReference": "desktop-client",
                "startedAt": "2026-08-02T09:00:00",
                "endedAt": "2026-08-02T09:45:00",
                "durationSeconds": 2700,
                "amountConsumed": 15,
                "status": "COMPLETED",
                "metadata": "manual end"
              },
              "consumption": {
                "usage": {
                  "id": "b2c3d4e5-0000-4000-8000-000000000002",
                  "userId": "2f07c5b2-4f0d-4090-86c1-021e5f6b80f8",
                  "amountConsumed": 15,
                  "actionType": "SESSION_ACTION",
                  "timestamp": "2026-08-02T09:45:00"
                },
                "quota": {
                  "id": "0f8b5201-8f39-4ea8-9c52-1e67ef5d00e1",
                  "userId": "2f07c5b2-4f0d-4090-86c1-021e5f6b80f8",
                  "dailyLimit": 100,
                  "usedToday": 50,
                  "remainingToday": 50,
                  "lastResetDate": "2026-08-02",
                  "penaltyLevel": 0
                }
              }
            }
            """;

    private final UsageSessionService usageSessionService;

    @Operation(
            summary = "Start a usage session",
            description = """
                    Starts a usage session for the authenticated user. Only one active session is
                    allowed per user (409 on conflict). Publishes a `SessionStartedEvent`.
                    """,
            operationId = "startSession"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Session start details. Both fields are optional.",
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = StartUsageSessionRequest.class),
                    examples = @ExampleObject(name = "startSessionRequest", summary = "Start a session",
                            value = "{\"clientReference\":\"desktop-client\"}"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Session started.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UsageSessionResponse.class),
                            examples = @ExampleObject(name = "startedSession", summary = "Active session",
                                    value = ACTIVE_SESSION_EXAMPLE))),
            @ApiResponse(responseCode = "400", ref = "BadRequest"),
            @ApiResponse(responseCode = "401", ref = "Unauthorized"),
            @ApiResponse(responseCode = "409", ref = "Conflict")
    })
    @PostMapping("/start")
    public ResponseEntity<UsageSessionResponse> startSession(
            @Valid @RequestBody StartUsageSessionRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usageSessionService.startSession(request));
    }

    @Operation(
            summary = "End a usage session",
            description = """
                    Ends an active usage session. If `amountConsumed` is omitted, consumption is
                    calculated from the session duration. Publishes a `SessionCompletedEvent`.
                    If the end-of-session consumption is rejected (quota exceeded or active
                    penalty), the session remains active and can be ended again later.
                    """,
            operationId = "endSession"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Session end details. `amountConsumed` and `metadata` are optional.",
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = EndUsageSessionRequest.class),
                    examples = @ExampleObject(name = "endSessionRequest", summary = "End with explicit amount",
                            value = "{\"amountConsumed\":15,\"metadata\":\"manual end\"}"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session ended and consumption recorded.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EndUsageSessionResponse.class),
                            examples = @ExampleObject(name = "endedSession", summary = "Ended session",
                                    value = ENDED_SESSION_EXAMPLE))),
            @ApiResponse(responseCode = "400", ref = "BadRequest"),
            @ApiResponse(responseCode = "401", ref = "Unauthorized"),
            @ApiResponse(responseCode = "404", ref = "NotFound"),
            @ApiResponse(responseCode = "409", ref = "Conflict")
    })
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<EndUsageSessionResponse> endSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody EndUsageSessionRequest request
    ) {
        return ResponseEntity.ok(usageSessionService.endSession(sessionId, request));
    }

    @Operation(
            summary = "Get the current active session",
            description = "Returns the authenticated user's active session, or 204 when none is active.",
            operationId = "getActiveSession"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "An active session exists.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UsageSessionResponse.class),
                            examples = @ExampleObject(name = "activeSession", summary = "Active session",
                                    value = ACTIVE_SESSION_EXAMPLE))),
            @ApiResponse(responseCode = "204", description = "No active session."),
            @ApiResponse(responseCode = "401", ref = "Unauthorized")
    })
    @GetMapping("/active")
    public ResponseEntity<UsageSessionResponse> activeSession() {
        return usageSessionService.getActiveSession()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(
            summary = "List usage session history",
            description = """
                    Returns a page of the authenticated user's usage sessions, most recent first.
                    Supports pagination via the `page` and `size` query parameters.
                    """,
            operationId = "getSessionHistory"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of usage sessions.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", ref = "BadRequest"),
            @ApiResponse(responseCode = "401", ref = "Unauthorized")
    })
    @GetMapping("/history")
    public ResponseEntity<Page<UsageSessionResponse>> history(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @AllowedPageSize int size
    ) {
        return ResponseEntity.ok(usageSessionService.getHistory(page, size));
    }
}
