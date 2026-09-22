package com.gianmdp03.cuando_llega_pro.domain.dashboard;

import com.gianmdp03.cuando_llega_pro.domain.dashboard.dto.DashboardPresetArrivalDTO;
import com.gianmdp03.cuando_llega_pro.domain.dashboard.dto.DashboardResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.preset.Preset;
import com.gianmdp03.cuando_llega_pro.domain.preset.PresetRepository;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.ArrivalResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.model.TelemetryStatus;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.service.ArrivalsService;
import com.gianmdp03.cuando_llega_pro.domain.user.User;
import com.gianmdp03.cuando_llega_pro.domain.user.UserRepository;
import com.gianmdp03.cuando_llega_pro.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Backend-for-Frontend (BFF) aggregation service providing real-time dashboard telemetry
 * across all configured transit presets for a user using Java 25 Virtual Thread fan-out.
 */
@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final PresetRepository presetRepository;
    private final UserRepository userRepository;
    private final ArrivalsService arrivalsService;

    public DashboardService(
            PresetRepository presetRepository,
            UserRepository userRepository,
            ArrivalsService arrivalsService
    ) {
        this.presetRepository = presetRepository;
        this.userRepository = userRepository;
        this.arrivalsService = arrivalsService;
    }

    /**
     * Aggregates arrival telemetry concurrently across all presets configured for the specified user.
     *
     * @param email authenticated user's email address
     * @return consolidated dashboard response containing telemetry for each preset
     * @throws ResourceNotFoundException if no user exists with the provided email
     */
    public DashboardResponseDTO getDashboardForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        List<Preset> presets = presetRepository.findAllByUserIdWithUser(user.getId());

        if (presets.isEmpty()) {
            return new DashboardResponseDTO(email, Instant.now(), 0, List.of());
        }

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<DashboardPresetArrivalDTO>> futures = presets.stream()
                    .map(preset -> CompletableFuture.supplyAsync(() -> resolvePresetTelemetry(preset), executor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            List<DashboardPresetArrivalDTO> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            return new DashboardResponseDTO(email, Instant.now(), results.size(), results);
        }
    }

    private DashboardPresetArrivalDTO resolvePresetTelemetry(Preset preset) {
        try {
            ArrivalResponseDTO telemetry = arrivalsService.getArrivals(
                    preset.getCodigoLinea(),
                    preset.getIdentificadorParada()
            );
            return new DashboardPresetArrivalDTO(
                    preset.getId(),
                    preset.getCodigoLinea(),
                    preset.getIdentificadorParada(),
                    preset.getBandera(),
                    preset.getConfig(),
                    filterTelemetryByBranch(telemetry, preset.getBandera()),
                    null
            );
        } catch (RuntimeException exception) {
            log.warn("Dashboard telemetry unavailable for presetId={} lineCode={} stopId={}: {}",
                    preset.getId(), preset.getCodigoLinea(), preset.getIdentificadorParada(), exception.toString());
            return new DashboardPresetArrivalDTO(
                    preset.getId(),
                    preset.getCodigoLinea(),
                    preset.getIdentificadorParada(),
                    preset.getBandera(),
                    preset.getConfig(),
                    ArrivalResponseDTO.empty(
                            preset.getCodigoLinea(),
                            preset.getIdentificadorParada(),
                            TelemetryStatus.UNAVAILABLE
                    ),
                    "Información temporalmente no disponible."
            );
        }
    }

    /**
     * Filters real-time arrival telemetry to only those matching the configured preset branch.
     * If the preset bandera is empty or null, all branches are included.
     *
     * @param telemetry     telemetry snapshot from arrivals service
     * @param targetBandera target branch / variant from preset
     * @return branch-filtered ArrivalResponseDTO
     */
    private ArrivalResponseDTO filterTelemetryByBranch(ArrivalResponseDTO telemetry, String targetBandera) {
        if (telemetry == null || targetBandera == null || targetBandera.isBlank()) {
            return telemetry;
        }

        List<com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.BusArrivalItemDTO> filtered =
                telemetry.arrivals().stream()
                        .filter(item -> item.branch() != null && item.branch().trim().equalsIgnoreCase(targetBandera.trim()))
                        .toList();

        return new ArrivalResponseDTO(
                telemetry.lineCode(),
                telemetry.stopId(),
                targetBandera.trim(),
                telemetry.status(),
                telemetry.timestamp(),
                telemetry.deltaMinutes(),
                filtered,
                telemetry.stopLatitude(),
                telemetry.stopLongitude()
        );
    }
}
