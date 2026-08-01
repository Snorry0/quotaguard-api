package com.snor.quotaguard.penalty.controller;

import com.snor.quotaguard.penalty.dto.response.PenaltyEventResponse;
import com.snor.quotaguard.penalty.service.PenaltyService;
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

    @GetMapping
    public ResponseEntity<List<PenaltyEventResponse>> getPenalties() {
        return ResponseEntity.ok(penaltyService.getCurrentUserPenaltyHistory());
    }
}
