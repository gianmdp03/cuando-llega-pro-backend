package com.gianmdp03.cuando_llega_pro.exception;

/**
 * Exception thrown when an upstream proxy or external service dependency fails.
 */
public class UpstreamServiceException extends RuntimeException {

    public UpstreamServiceException(String message) {
        super(message);
    }

    public UpstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
