package com.snor.quotaguard.penalty.controller;

import com.snor.quotaguard.penalty.dto.response.PenaltyEventResponse;
import com.snor.quotaguard.penalty.service.PenaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(
        name = "Penalties",
        description = "Active and historical penalty state endpoints"
)
@RestController
@RequestMapping("/api/v1/penalties")
@RequiredArgsConstructor
public class PenaltyController {

    private static final String PENALTIES_EXAMPLE = """
            [
              {
                "id": "c3d4e5f6-0000-4000-8000-000000000003",
                "userId": "2f07c5b2-4f0d-4090-86c1-021e5f6b80f8",
                "type": "SHORT_COOLDOWN",
                "startTime": "2026-08-02T10:20:00",
                "endTime": "2026-08-02T10:35:00",
                "active": true
              }
            ]
            """;

    private final PenaltyService penaltyService;

    @Operation(
            summary = "List the current user's penalties",
            description = "Returns the authenticated user's active and historical penalty events, most recent first.",
            operationId = "getCurrentPenalties"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The user's penalty history.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PenaltyEventResponse.class),
                            examples = @ExampleObject(name = "penalties", summary = "Penalty history",
                                    value = PENALTIES_EXAMPLE))),
            @ApiResponse(responseCode = "401", ref = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<List<PenaltyEventResponse>> getPenalties() {
        return ResponseEntity.ok(penaltyService.getCurrentUserPenaltyHistory());
    }
}
