package com.snor.quotaguard.audit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Flat pagination envelope used by the audit endpoints.
 */
@Schema(
        description = """
                Flat pagination envelope used by the audit endpoints. The concrete element schema of
                `content` is resolved per endpoint (for example `AuditEventResponse` for
                `GET /api/v1/audit`).
                """
)
public record PageResponse<T>(
        @Schema(
                description = "Page elements. The element schema is set per endpoint (for example `AuditEventResponse`)."
        )
        List<T> content,
        @Schema(
                description = "Zero-based page index.",
                example = "0"
        )
        int page,
        @Schema(
                description = "Number of elements in this page.",
                example = "20"
        )
        int size,
        @Schema(
                description = "Total number of elements across all pages.",
                example = "42"
        )
        long totalElements,
        @Schema(
                description = "Total number of pages.",
                example = "3"
        )
        int totalPages
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
