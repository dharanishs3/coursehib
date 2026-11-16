package com.coursehub.exception;

/** Thrown when a logged-in user is not allowed to touch a record (for example not the owner). Mapped to HTTP 403. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
