package com.snor.quotaguard.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Severity of a quota penalty applied to a user.
 */
@Schema(
        description = """
                Severity of a quota penalty applied to a user.

                Possible values:
                - `WARNING` — Advisory penalty; does not block consumption but is recorded on the user.
                - `SHORT_COOLDOWN` — Short temporary cooldown (default 15 minutes) blocking consumption; `Retry-After` header returned on blocked requests.
                - `LONG_COOLDOWN` — Long temporary cooldown (default 4 hours) blocking consumption; `Retry-After` header returned on blocked requests.
                """
)
public enum PenaltyType {
    WARNING,
    SHORT_COOLDOWN,
    LONG_COOLDOWN
}
