package com.snor.quotaguard.session.dto.request;

import com.snor.quotaguard.validation.annotation.Trimmed;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Payload for ending an active usage session.
 */
@Schema(
        description = """
                Payload for ending an active usage session. The session's consumption is finalized and
                the session transitions to the `COMPLETED` state.
                """
)
public record EndUsageSessionRequest(
        @Schema(
                description = "Optional explicit amount to consume. If omitted, backend calculates it from session duration.",
                example = "15",
                minimum = "1",
                nullable = true
        )
        @Positive
        Integer amountConsumed,

        @Schema(
                description = "Optional free-form metadata attached to the session end.",
                example = "manual end"
        )
        @Size(max = 5000)
        @Trimmed
        String metadata
) {
}
