package com.snor.quotaguard.usage.controller;

import com.snor.quotaguard.usage.dto.request.ConsumeUsageRequest;
import com.snor.quotaguard.usage.dto.response.ConsumeUsageResponse;
import com.snor.quotaguard.usage.dto.response.UsageRecordResponse;
import com.snor.quotaguard.usage.service.UsageService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Usage",
        description = "Resource consumption and usage history endpoints"
)
@Validated
@RestController
@RequestMapping("/api/v1/usage")
@RequiredArgsConstructor
public class UsageController {

    private static final String CONSUME_RESPONSE_EXAMPLE = """
            {
              "usage": {
                "id": "b2c3d4e5-0000-4000-8000-000000000002",
                "userId": "2f07c5b2-4f0d-4090-86c1-021e5f6b80f8",
                "amountConsumed": 10,
                "actionType": "API_CALL",
                "timestamp": "2026-08-02T10:20:00"
              },
              "quota": {
                "id": "0f8b5201-8f39-4ea8-9c52-1e67ef5d00e1",
                "userId": "2f07c5b2-4f0d-4090-86c1-021e5f6b80f8",
                "dailyLimit": 100,
                "usedToday": 35,
                "remainingToday": 65,
                "lastResetDate": "2026-08-02",
                "penaltyLevel": 0
              }
            }
            """;

    private final UsageService usageService;

    @Operation(
            summary = "Consume resource units",
            description = """
                    Attempts to consume resource units for the authenticated user.
                    If the user exceeds the daily quota, a progressive penalty may be applied.
                    A consumption that exceeds the remaining quota, or is attempted while a
                    penalty is active, is rejected with `429 Too Many Requests`.
                    """,
            operationId = "consumeUsage"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "The amount and action type of the consumption.",
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ConsumeUsageRequest.class),
                    examples = @ExampleObject(name = "consumeRequest", summary = "Consume 10 API_CALL units",
                            value = "{\"amountConsumed\":10,\"actionType\":\"API_CALL\"}"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consumption recorded and quota updated.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ConsumeUsageResponse.class),
                            examples = @ExampleObject(name = "consumedUsage",
                                    summary = "Recorded consumption and updated quota",
                                    value = CONSUME_RESPONSE_EXAMPLE))),
            @ApiResponse(responseCode = "400", ref = "BadRequest"),
            @ApiResponse(responseCode = "401", ref = "Unauthorized"),
            @ApiResponse(responseCode = "429", ref = "TooManyRequests")
    })
    @PostMapping("/consume")
    public ResponseEntity<ConsumeUsageResponse> consume(
            @Valid @RequestBody ConsumeUsageRequest request
    ) {
        return ResponseEntity.ok(usageService.consume(request));
    }

    @Operation(
            summary = "List usage history",
            description = """
                    Returns the authenticated user's historical usage records.
                    Supports pagination via the `page` and `size` query parameters.
                    """,
            operationId = "getUsageHistory"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of usage records.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", ref = "BadRequest"),
            @ApiResponse(responseCode = "401", ref = "Unauthorized")
    })
    @GetMapping("/history")
    public ResponseEntity<Page<UsageRecordResponse>> history(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @AllowedPageSize int size
    ) {
        return ResponseEntity.ok(usageService.getHistory(page, size));
    }
}
