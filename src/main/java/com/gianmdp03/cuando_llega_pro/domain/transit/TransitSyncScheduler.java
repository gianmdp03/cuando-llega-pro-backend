package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gianmdp03.cuando_llega_pro.client.MgpProxyClient;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.Linea;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.LineaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.gianmdp03.cuando_llega_pro.config.CaffeineCacheConfig;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Refreshes stop coordinates and adds newly published stops without deleting catalogue records. */
@Component
@RequiredArgsConstructor
public class TransitSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(TransitSyncScheduler.class);

    private final LineaRepository lineaRepository;
    private final MgpProxyClient mgpProxyClient;
    private final ObjectMapper objectMapper;
    private final TransitDataPersistenceService transitDataPersistenceService;
    private final CacheManager cacheManager;

    @Scheduled(cron = "0 0 4 * * *")
    public void synchronize() {
        boolean completedSuccessfully = true;
        for (Linea linea : lineaRepository.findAll()) {
            try {
                String response = mgpProxyClient.getStopsByLine(
                        UUID.randomUUID().toString(),
                        TransitCatalogService.ACTION_RECUPERAR_PARADAS_LEGACY,
                        linea.getCodigo()
                );
                JsonNode payload = objectMapper.readTree(response);
                transitDataPersistenceService.synchronizeLineStops(linea, payload);
            } catch (Exception exception) {
                completedSuccessfully = false;
                log.warn("No se pudo sincronizar las paradas de la línea {}", linea.getCodigo(), exception);
            }
        }
        if (completedSuccessfully) {
            var cache = cacheManager.getCache(CaffeineCacheConfig.TRANSIT_MAP_CACHE);
            if (cache != null) {
                cache.clear();
            }
        } else {
            log.warn("La caché de mapa se conserva porque la sincronización no finalizó completamente");
        }
    }
}
