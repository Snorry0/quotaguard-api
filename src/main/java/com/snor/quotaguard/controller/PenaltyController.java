package com.snor.quotaguard.controller;

import com.snor.quotaguard.dto.response.PenaltyEventResponse;
import com.snor.quotaguard.security.CurrentUserProvider;
import com.snor.quotaguard.service.PenaltyService;
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

    private final PenaltyService penaltyService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<List<PenaltyEventResponse>> getPenalties() {
        return ResponseEntity.ok(penaltyService.getPenaltyHistory(currentUserProvider.getCurrentUser()));
    }
}
