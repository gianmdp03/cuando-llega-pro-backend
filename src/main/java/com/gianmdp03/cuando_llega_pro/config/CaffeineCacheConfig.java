package com.gianmdp03.cuando_llega_pro.config;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CaffeineCacheConfig {

    public static final String ARRIVALS_CACHE = "arrivals";
    public static final String LINES_CACHE = "lines";
    public static final String TRANSIT_CATALOG_CACHE = "transit-catalog";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheManager.registerCustomCache(
                ARRIVALS_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofSeconds(15))
                        .maximumSize(10_000)
                        .recordStats()
                        .build()
        );

        cacheManager.registerCustomCache(
                LINES_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofHours(1))
                        .maximumSize(500)
                        .recordStats()
                        .build()
        );

        cacheManager.registerCustomCache(
                TRANSIT_CATALOG_CACHE,
                Caffeine.newBuilder()
                        .maximumSize(2000)
                        .recordStats()
                        .build()
        );

        return cacheManager;
    }
}
