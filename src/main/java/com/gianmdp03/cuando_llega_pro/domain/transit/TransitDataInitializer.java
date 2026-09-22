package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.TransitStopRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/** Loads the bundled static transit catalogue only when no physical stops exist yet. */
@Component
@ConditionalOnProperty(name = "app.transit.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class TransitDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TransitDataInitializer.class);
    private static final int BATCH_SIZE = 200;

    private final TransitStopRepository stopRepository;
    private final TransitDataPersistenceService transitDataPersistenceService;
    private final PublishedTransitCatalogImporter publishedTransitCatalogImporter;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean importedFromGitHub = publishedTransitCatalogImporter.importNow();
        if (importedFromGitHub || stopRepository.count() > 0) {
            return;
        }

        ClassPathResource resource = new ClassPathResource("paradas_mgp.json");
        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode dataset = objectMapper.readTree(inputStream);
            transitDataPersistenceService.replaceCatalogSnapshot(dataset, BATCH_SIZE);
            log.info("GitHub no estuvo disponible; catálogo local inicializado con {} stops", stopRepository.count());
        }
    }
}
