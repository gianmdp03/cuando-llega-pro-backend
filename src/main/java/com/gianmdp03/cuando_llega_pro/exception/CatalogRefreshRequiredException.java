package com.gianmdp03.cuando_llega_pro.exception;

/** Signals that a municipal catalogue entry must be refreshed by a mobile client. */
public class CatalogRefreshRequiredException extends RuntimeException {
    public CatalogRefreshRequiredException(String cacheKey) {
        super("El catálogo municipal venció y debe ser renovado por un cliente móvil: " + cacheKey);
    }
}
