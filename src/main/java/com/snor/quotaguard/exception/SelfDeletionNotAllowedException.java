package com.snor.quotaguard.exception;

public class SelfDeletionNotAllowedException extends RuntimeException {

    public SelfDeletionNotAllowedException() {
        super("Admins cannot delete their own account");
    }
}
