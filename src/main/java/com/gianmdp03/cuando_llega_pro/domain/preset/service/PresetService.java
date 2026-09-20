package com.gianmdp03.cuando_llega_pro.domain.preset.service;

import com.gianmdp03.cuando_llega_pro.domain.preset.Preset;
import com.gianmdp03.cuando_llega_pro.domain.preset.PresetRepository;
import com.gianmdp03.cuando_llega_pro.domain.preset.dto.PresetDetailDTO;
import com.gianmdp03.cuando_llega_pro.domain.preset.dto.PresetListDTO;
import com.gianmdp03.cuando_llega_pro.domain.preset.dto.PresetRequestDTO;
import com.gianmdp03.cuando_llega_pro.domain.user.User;
import com.gianmdp03.cuando_llega_pro.domain.user.UserRepository;
import com.gianmdp03.cuando_llega_pro.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing user presets lifecycle, queries, modifications, and deletions.
 * Enforces user ownership boundaries on every operation.
 */
@Service
@Transactional(readOnly = true)
public class PresetService {

    private static final Logger log = LoggerFactory.getLogger(PresetService.class);

    private final PresetRepository presetRepository;
    private final UserRepository userRepository;

    public PresetService(PresetRepository presetRepository, UserRepository userRepository) {
        this.presetRepository = presetRepository;
        this.userRepository = userRepository;
    }

    /**
     * Retrieves all presets belonging to the specified user email.
     *
     * @param email authenticated user's email
     * @return list of preset summaries
     */
    public List<PresetListDTO> getPresetsForUser(String email) {
        log.debug("Retrieving presets for user email: {}", email);
        User user = findUserByEmail(email);

        return presetRepository.findAllByUserIdWithUser(user.getId())
                .stream()
                .map(PresetListDTO::fromEntity)
                .toList();
    }

    /**
     * Retrieves a single preset by ID belonging to the specified user email.
     *
     * @param presetId ID of the preset
     * @param email    authenticated user's email
     * @return detailed preset representation
     */
    public PresetDetailDTO getPresetByIdForUser(Long presetId, String email) {
        log.debug("Retrieving preset id: {} for user email: {}", presetId, email);
        User user = findUserByEmail(email);

        Preset preset = presetRepository.findByIdAndUserIdWithUser(presetId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Preset not found with id: " + presetId));

        return PresetDetailDTO.fromEntity(preset);
    }

    /**
     * Creates a new preset for the authenticated user.
     *
     * @param email   authenticated user's email
     * @param request preset creation payload
     * @return created preset details
     */
    @Transactional
    public PresetDetailDTO createPresetForUser(String email, PresetRequestDTO request) {
        log.info("Creating preset for user email: {}, line: {}, stop: {}",
                email, request.codigoLinea(), request.identificadorParada());
        User user = findUserByEmail(email);

        Preset preset = new Preset(
                user,
                request.codigoLinea(),
                request.identificadorParada(),
                request.bandera(),
                request.config()
        );

        Preset saved = presetRepository.save(preset);
        return PresetDetailDTO.fromEntity(saved);
    }

    /**
     * Updates an existing preset owned by the authenticated user.
     *
     * @param presetId ID of the preset to update
     * @param email    authenticated user's email
     * @param request  preset update payload
     * @return updated preset details
     */
    @Transactional
    public PresetDetailDTO updatePresetForUser(Long presetId, String email, PresetRequestDTO request) {
        log.info("Updating preset id: {} for user email: {}", presetId, email);
        User user = findUserByEmail(email);

        Preset preset = presetRepository.findByIdAndUserIdWithUser(presetId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Preset not found with id: " + presetId));

        preset.setCodigoLinea(request.codigoLinea());
        preset.setIdentificadorParada(request.identificadorParada());
        preset.setBandera(request.bandera());
        preset.setConfig(request.config());

        Preset updated = presetRepository.save(preset);
        return PresetDetailDTO.fromEntity(updated);
    }

    /**
     * Deletes an existing preset owned by the authenticated user.
     *
     * @param presetId ID of the preset to delete
     * @param email    authenticated user's email
     */
    @Transactional
    public void deletePresetForUser(Long presetId, String email) {
        log.info("Deleting preset id: {} for user email: {}", presetId, email);
        User user = findUserByEmail(email);

        Preset preset = presetRepository.findByIdAndUserIdWithUser(presetId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Preset not found with id: " + presetId));

        presetRepository.delete(preset);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
