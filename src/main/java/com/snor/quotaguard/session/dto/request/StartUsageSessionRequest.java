package com.snor.quotaguard.session.dto.request;

import com.snor.quotaguard.validation.annotation.Trimmed;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Payload for starting a new usage session.
 */
@Schema(
        description = """
                Payload for starting a new usage session. The session is opened in the `ACTIVE` state
                and records consumption until it is ended.
                """
)
public record StartUsageSessionRequest(
        @Schema(
                description = "Optional client-provided reference identifying the session.",
                example = "desktop-client"
        )
        @Size(max = 128)
        @Trimmed
        String clientReference,

        @Schema(
                description = "Optional free-form metadata attached to the session.",
                example = "started from desktop client"
        )
        @Size(max = 5000)
        @Trimmed
        String metadata
) {
}
