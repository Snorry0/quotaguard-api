package com.snor.quotaguard.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lifecycle state of a usage session.
 */
@Schema(
        description = """
                Lifecycle state of a usage session.

                Possible values:
                - `ACTIVE` — Session is open and accepting consumption.
                - `COMPLETED` — Session has been ended and its consumption recorded.
                """
)
public enum SessionStatus {
    ACTIVE,
    COMPLETED
}
