package com.gianmdp03.cuando_llega_pro.exception;

/**
 * Encapsulates field validation error details.
 *
 * @param field the name of the rejected field
 * @param rejectedValue the value that failed validation
 * @param message description of the validation failure
 */
public record ValidationError(
        String field,
        Object rejectedValue,
        String message
) {
}
