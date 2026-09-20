package com.gianmdp03.cuando_llega_pro.domain.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Administrative controller exposing operations for cache invalidation and maintenance.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminCacheController {

    private static final Logger log = LoggerFactory.getLogger(AdminCacheController.class);

    private final CacheManager cacheManager;

    public AdminCacheController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Clears one or all registered application cache buckets.
     *
     * @param name optional cache name to invalidate; if omitted, all registered caches are cleared
     * @return 200 OK with operation status and list of purged cache names
     */
    @DeleteMapping("/cache")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> clearCache(@RequestParam(required = false) String name) {
        List<String> purgedCaches = new ArrayList<>();

        if (name != null && !name.isBlank()) {
            String cacheName = name.trim();
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                purgedCaches.add(cacheName);
                log.info("Admin cache invalidated: {}", cacheName);
            } else {
                log.warn("Admin cache invalidation requested for unknown cache: {}", cacheName);
            }
        } else {
            for (String cacheName : cacheManager.getCacheNames()) {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    purgedCaches.add(cacheName);
                }
            }
            log.info("Admin cleared all registered caches: {}", purgedCaches);
        }

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "purgedCaches", purgedCaches
        ));
    }
}
