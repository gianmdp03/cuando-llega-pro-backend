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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Service providing user presets for the dashboard.
 * Real-time arrival telemetry is decoupled from VPS backend and delegated to the client mobile worker.
 */
@Service
public class DashboardService {

    private final PresetRepository presetRepository;
    private final UserRepository userRepository;
    private final ArrivalsService arrivalsService;

    @Autowired
    public DashboardService(
            PresetRepository presetRepository,
            UserRepository userRepository,
            @Autowired(required = false) ArrivalsService arrivalsService
    ) {
        this.presetRepository = presetRepository;
        this.userRepository = userRepository;
        this.arrivalsService = arrivalsService;
    }

    public DashboardService(
            PresetRepository presetRepository,
            UserRepository userRepository
    ) {
        this(presetRepository, userRepository, null);
    }

    /**
     * Retrieves presets for the specified user, returning metadata and delegating telemetry to client.
     *
     * @param email authenticated user's email address
     * @return consolidated dashboard response containing presets
     * @throws ResourceNotFoundException if no user exists with the provided email
     */
    public DashboardResponseDTO getDashboardForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        List<Preset> presets = presetRepository.findAllByUserIdWithUser(user.getId());

        if (presets.isEmpty()) {
            return new DashboardResponseDTO(email, Instant.now(), 0, List.of());
        }

        List<DashboardPresetArrivalDTO> results = presets.stream()
                .map(this::resolvePresetTelemetry)
                .toList();

        return new DashboardResponseDTO(email, Instant.now(), results.size(), results);
    }

    private DashboardPresetArrivalDTO resolvePresetTelemetry(Preset preset) {
        return new DashboardPresetArrivalDTO(
                preset.getId(),
                preset.getCodigoLinea(),
                preset.getIdentificadorParada(),
                preset.getBandera(),
                preset.getConfig(),
                ArrivalResponseDTO.empty(
                        preset.getCodigoLinea(),
                        preset.getIdentificadorParada(),
                        TelemetryStatus.DELEGATED_TO_CLIENT
                ),
                null
        );
    }
}
