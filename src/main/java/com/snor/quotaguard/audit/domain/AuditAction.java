package com.snor.quotaguard.audit.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Action recorded in the audit trail.
 */
@Schema(
        description = """
                Action recorded in the audit trail.

                Possible values:
                - `USER_CREATED` — A new user account was created.
                - `USER_UPDATED` — An existing user account was updated.
                - `USER_DELETED` — A user account was deleted.
                - `LOGIN_SUCCESS` — A user successfully authenticated.
                - `LOGIN_FAILED` — A login attempt failed (invalid credentials).
                - `REGISTER_SUCCESS` — A new user was successfully registered.
                - `REGISTER_FAILED` — A registration attempt failed (typically duplicate email).
                - `QUOTA_RESET` — All quotas were reset (typically by an admin).
                - `PENALTY_APPLIED` — A penalty was applied to a user.
                - `PENALTY_EXPIRED` — A penalty expired and is no longer blocking.
                - `SESSION_STARTED` — A usage session was started.
                - `SESSION_COMPLETED` — A usage session was completed.
                """
)
public enum AuditAction {
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    REGISTER_SUCCESS,
    REGISTER_FAILED,
    QUOTA_RESET,
    PENALTY_APPLIED,
    PENALTY_EXPIRED,
    SESSION_STARTED,
    SESSION_COMPLETED
}
