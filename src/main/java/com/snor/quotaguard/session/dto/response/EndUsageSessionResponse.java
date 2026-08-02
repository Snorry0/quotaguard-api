package com.snor.quotaguard.session.dto.response;

import com.snor.quotaguard.usage.dto.response.ConsumeUsageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Result of ending a usage session.
 */
@Schema(
        description = """
                Result of ending a usage session: the completed session and the consumption recorded
                against it, together with the updated quota state.
                """
)
public record EndUsageSessionResponse(
        @Schema(
                description = "The completed usage session."
        )
        UsageSessionResponse session,
        @Schema(
                description = "The consumption recorded for the session and the updated quota."
        )
        ConsumeUsageResponse consumption
) {
}
