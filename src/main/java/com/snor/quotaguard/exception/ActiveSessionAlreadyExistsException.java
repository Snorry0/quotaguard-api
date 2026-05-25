package com.snor.quotaguard.exception;

import lombok.Data;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ActiveSessionAlreadyExistsException extends RuntimeException {

    private final UUID sessionId;

    public ActiveSessionAlreadyExistsException(UUID sessionId) {
        super("An active usage session already exists");
        this.sessionId = sessionId;
    }
}