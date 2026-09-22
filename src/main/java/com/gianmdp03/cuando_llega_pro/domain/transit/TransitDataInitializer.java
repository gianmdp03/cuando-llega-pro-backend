package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.TransitStopRepository;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.TransitRouteRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

/** Loads the bundled static transit catalogue only when no physical stops exist yet. */
@Component
@ConditionalOnProperty(name = "app.transit.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class TransitDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TransitDataInitializer.class);
    private static final int BATCH_SIZE = 200;

    private final TransitStopRepository stopRepository;
    private final TransitRouteRepository routeRepository;
    private final TransitDataPersistenceService transitDataPersistenceService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        ClassPathResource resource = new ClassPathResource("paradas_mgp.json");
        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode dataset = objectMapper.readTree(inputStream);
            if (stopRepository.count() == 0) {
                transitDataPersistenceService.replaceEmptyCatalog(dataset, BATCH_SIZE);
                log.info("Catálogo estático de transporte inicializado con {} stops", stopRepository.count());
            } else if (routeRepository.count() == 0) {
                transitDataPersistenceService.replaceRoutes(dataset);
                log.info("Geometrías estáticas de transporte inicializadas con {} recorridos", routeRepository.count());
            }
        }
    }
}
