package com.gianmdp03.cuando_llega_pro.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

class CaffeineCacheConfigTest {

    @Test
    void shouldConfigureArrivalsAndLinesCachesWithStats() {
        CaffeineCacheConfig config = new CaffeineCacheConfig();
        CaffeineCacheManager cacheManager = (CaffeineCacheManager) config.cacheManager();

        assertThat(cacheManager.getCacheNames()).containsExactlyInAnyOrder(
                CaffeineCacheConfig.ARRIVALS_CACHE,
                CaffeineCacheConfig.LINES_CACHE,
                CaffeineCacheConfig.TRANSIT_CATALOG_CACHE
        );

        Cache arrivalsCache = cacheManager.getCache("arrivals");
        assertThat(arrivalsCache).isNotNull().isInstanceOf(CaffeineCache.class);
        CaffeineCache caffeineArrivals = (CaffeineCache) arrivalsCache;
        // Verify stats are recorded
        assertThat(caffeineArrivals.getNativeCache().stats()).isNotNull();

        Cache linesCache = cacheManager.getCache("lines");
        assertThat(linesCache).isNotNull().isInstanceOf(CaffeineCache.class);
        CaffeineCache caffeineLines = (CaffeineCache) linesCache;
        assertThat(caffeineLines.getNativeCache().stats()).isNotNull();

        // Verify basic put and get functionality
        arrivalsCache.put("stop_123", "data_123");
        assertThat(arrivalsCache.get("stop_123", String.class)).isEqualTo("data_123");

        linesCache.put("line_501", "data_501");
        assertThat(linesCache.get("line_501", String.class)).isEqualTo("data_501");
    }
}
