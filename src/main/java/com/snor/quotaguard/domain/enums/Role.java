package com.snor.quotaguard.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * User role governing access to the API.
 */
@Schema(
        description = """
                User role governing access to the API.

                Possible values:
                - `USER` — Standard user account with self-service access to the user's own profile, quota, usage, sessions, and penalties.
                - `ADMIN` — Administrator account with full access to all user-administration, quota-reset, and audit-trail endpoints.
                """
)
public enum Role {
    USER,
    ADMIN
}
