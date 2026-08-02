package com.snor.quotaguard.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Type of action that consumes resource units.
 */
@Schema(
        description = """
                Type of action that consumes resource units.

                Possible values:
                - `API_CALL` — Programmatic API call consuming resource units.
                - `RESOURCE_ACCESS` — Access to a managed resource (file, asset, etc.) consuming units.
                - `BACKGROUND_JOB` — Scheduled or background job consuming units.
                - `SESSION_ACTION` — Event recorded against an active usage session.
                - `MANUAL_ADJUSTMENT` — Administrative correction (positive or negative) applied to a user's quota.
                """
)
public enum ActionType {
    API_CALL,
    RESOURCE_ACCESS,
    BACKGROUND_JOB,
    SESSION_ACTION,
    MANUAL_ADJUSTMENT
}
