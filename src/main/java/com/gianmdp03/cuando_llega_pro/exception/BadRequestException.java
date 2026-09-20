package com.gianmdp03.cuando_llega_pro.exception;

/**
 * Exception thrown when a client provides an invalid request payload or parameters.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
