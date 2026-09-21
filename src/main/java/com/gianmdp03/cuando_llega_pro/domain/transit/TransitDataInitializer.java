package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.TransitStopRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

/** Loads the bundled static transit catalogue only when no physical stops exist yet. */
@Component
@RequiredArgsConstructor
public class TransitDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TransitDataInitializer.class);
    private static final int BATCH_SIZE = 200;

    private final TransitStopRepository stopRepository;
    private final TransitDataPersistenceService transitDataPersistenceService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (stopRepository.count() > 0) {
            return;
        }

        ClassPathResource resource = new ClassPathResource("stops_mgp.json");
        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode dataset = objectMapper.readTree(inputStream);
            transitDataPersistenceService.replaceEmptyCatalog(dataset, BATCH_SIZE);
            log.info("Catálogo estático de transporte inicializado con {} stops", stopRepository.count());
        }
    }
}
