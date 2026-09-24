package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

/** Normalized catalogue data collected by an authenticated mobile MGP session. */
public record CatalogRefreshRequest(String cacheKey, String catalogType, String payload) {}
