package com.snor.quotaguard.auth.exception;

/**
 * Signals an invalid, expired, revoked or replayed refresh token. Mapped by the
 * {@code GlobalExceptionHandler} to a generic {@code 401} so the client cannot
 * distinguish the exact failure reason (no leaked implementation details).
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
