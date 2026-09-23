package com.gianmdp03.cuando_llega_pro.domain.preset.service;

import com.gianmdp03.cuando_llega_pro.domain.preset.Preset;
import com.gianmdp03.cuando_llega_pro.domain.preset.PresetRepository;
import com.gianmdp03.cuando_llega_pro.domain.preset.dto.PresetDetailDTO;
import com.gianmdp03.cuando_llega_pro.domain.preset.dto.PresetListDTO;
import com.gianmdp03.cuando_llega_pro.domain.preset.dto.PresetRequestDTO;
import com.gianmdp03.cuando_llega_pro.domain.preset.model.PresetConfig;
import com.gianmdp03.cuando_llega_pro.domain.user.User;
import com.gianmdp03.cuando_llega_pro.domain.user.UserRepository;
import com.gianmdp03.cuando_llega_pro.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresetServiceTest {

    @Mock
    private PresetRepository presetRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PresetService presetService;

    private User sampleUser;
    private PresetConfig sampleConfig;
    private Preset samplePreset;

    @BeforeEach
    void setUp() {
        sampleUser = new User("tester@example.com", "secretPassword123", "Tester Name", "ROLE_USER");
        ReflectionTestUtils.setField(sampleUser, "id", 1L);

        sampleConfig = new PresetConfig(
                "Work",
                "bus-icon",
                "#00AAFF",
                null
        );

        samplePreset = new Preset(sampleUser, "501", "P-100", "A", sampleConfig);
        ReflectionTestUtils.setField(samplePreset, "id", 10L);
        ReflectionTestUtils.setField(samplePreset, "createdAt", Instant.parse("2026-01-01T12:00:00Z"));
    }

    @Nested
    @DisplayName("getPresetsForUser")
    class GetPresetsForUserTests {

        @Test
        @DisplayName("Throws ResourceNotFoundException when user is not found by email")
        void throwsExceptionWhenUserNotFound() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> presetService.getPresetsForUser("unknown@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with email: unknown@example.com");

            verify(presetRepository, never()).findAllByUserIdWithUser(any());
        }

        @Test
        @DisplayName("Returns mapped PresetListDTO list when presets exist")
        void returnsPresetListWhenFound() {
            when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(sampleUser));
            when(presetRepository.findAllByUserIdWithUser(1L)).thenReturn(List.of(samplePreset));

            List<PresetListDTO> result = presetService.getPresetsForUser("tester@example.com");

            assertThat(result).hasSize(1);
            PresetListDTO dto = result.getFirst();
            assertThat(dto.id()).isEqualTo(10L);
            assertThat(dto.codigoLinea()).isEqualTo("501");
            assertThat(dto.identificadorParada()).isEqualTo("P-100");
            assertThat(dto.bandera()).isEqualTo("A");
            assertThat(dto.alias()).isEqualTo("Work");
            assertThat(dto.icon()).isEqualTo("bus-icon");
            assertThat(dto.color()).isEqualTo("#00AAFF");
        }

        @Test
        @DisplayName("Returns empty list when user has no presets")
        void returnsEmptyListWhenNoPresets() {
            when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(sampleUser));
            when(presetRepository.findAllByUserIdWithUser(1L)).thenReturn(List.of());

            List<PresetListDTO> result = presetService.getPresetsForUser("tester@example.com");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getPresetByIdForUser")
    class GetPresetByIdForUserTests {

        @Test
        @DisplayName("Throws ResourceNotFoundException when user is not found")
        void throwsExceptionWhenUserNotFound() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> presetService.getPresetByIdForUser(10L, "unknown@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with email: unknown@example.com");

            verify(presetRepository, never()).findByIdAndUserIdWithUser(any(), any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when preset is not found for user")
        void throwsExceptionWhenPresetNotFound() {
            when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(sampleUser));
            when(presetRepository.findByIdAndUserIdWithUser(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> presetService.getPresetByIdForUser(99L, "tester@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Preset not found with id: 99");
        }

        @Test
        @DisplayName("Returns PresetDetailDTO when preset is found")
        void returnsPresetDetailWhenFound() {
            when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(sampleUser));
            when(presetRepository.findByIdAndUserIdWithUser(10L, 1L)).thenReturn(Optional.of(samplePreset));

            PresetDetailDTO detail = presetService.getPresetByIdForUser(10L, "tester@example.com");

            assertThat(detail).isNotNull();
            assertThat(detail.id()).isEqualTo(10L);
            assertThat(detail.userId()).isEqualTo(1L);
            assertThat(detail.codigoLinea()).isEqualTo("501");
            assertThat(detail.identificadorParada()).isEqualTo("P-100");
            assertThat(detail.bandera()).isEqualTo("A");
            assertThat(detail.config()).isEqualTo(sampleConfig);
            assertThat(detail.createdAt()).isEqualTo(Instant.parse("2026-01-01T12:00:00Z"));
        }
    }

    @Nested
    @DisplayName("createPresetForUser")
    class CreatePresetForUserTests {

        @Test
        @DisplayName("Throws ResourceNotFoundException when user is not found")
        void throwsExceptionWhenUserNotFound() {
            PresetRequestDTO request = new PresetRequestDTO("501", "P-100", "A", sampleConfig);
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> presetService.createPresetForUser("unknown@example.com", request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with email: unknown@example.com");

            verify(presetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Creates, saves, and returns PresetDetailDTO when successful")
        void createsAndSavesPresetSuccessfully() {
            PresetRequestDTO request = new PresetRequestDTO("502", "P-200", "B", sampleConfig);
            when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(sampleUser));

            Preset savedEntity = new Preset(sampleUser, "502", "P-200", "B", sampleConfig);
            ReflectionTestUtils.setField(savedEntity, "id", 25L);
            ReflectionTestUtils.setField(savedEntity, "createdAt", Instant.parse("2026-02-01T10:00:00Z"));

            when(presetRepository.save(any(Preset.class))).thenReturn(savedEntity);

            PresetDetailDTO result = presetService.createPresetForUser("tester@example.com", request);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(25L);
            assertThat(result.userId()).isEqualTo(1L);
            assertThat(result.codigoLinea()).isEqualTo("502");
            assertThat(result.identificadorParada()).isEqualTo("P-200");
            assertThat(result.bandera()).isEqualTo("B");
            assertThat(result.config()).isEqualTo(sampleConfig);

            ArgumentCaptor<Preset> captor = ArgumentCaptor.forClass(Preset.class);
            verify(presetRepository).save(captor.capture());
            Preset captured = captor.getValue();
            assertThat(captured.getUser()).isSameAs(sampleUser);
            assertThat(captured.getCodigoLinea()).isEqualTo("502");
            assertThat(captured.getIdentificadorParada()).isEqualTo("P-200");
            assertThat(captured.getBandera()).isEqualTo("B");
            assertThat(captured.getConfig()).isEqualTo(sampleConfig);
        }
    }

    @Nested
    @DisplayName("updatePresetForUser")
    class UpdatePresetForUserTests {

        @Test
        @DisplayName("Throws ResourceNotFoundException when user is not found")
        void throwsExceptionWhenUserNotFound() {
            PresetRequestDTO request = new PresetRequestDTO("501", "P-100", "A", sampleConfig);
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> presetService.updatePresetForUser(10L, "unknown@example.com", request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with email: unknown@example.com");

            verify(presetRepository, never()).findByIdAndUserIdWithUser(any(), any());
            verify(presetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when preset is not found")
        void throwsExceptionWhenPresetNotFound() {
            PresetRequestDTO request = new PresetRequestDTO("501", "P-100", "A", sampleConfig);
            when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(sampleUser));
            when(presetRepository.findByIdAndUserIdWithUser(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> presetService.updatePresetForUser(99L, "tester@example.com", request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Preset not found with id: 99");

            verify(presetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Updates preset fields, saves, and returns updated PresetDetailDTO")
        void updatesPresetSuccessfully() {
            PresetConfig updatedConfig = new PresetConfig(
                    "Gym",
                    "fitness-icon",
                    "#00FF00",
                    null
            );
            PresetRequestDTO request = new PresetRequestDTO("505", "P-555", "Express", updatedConfig);

            when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(sampleUser));
            when(presetRepository.findByIdAndUserIdWithUser(10L, 1L)).thenReturn(Optional.of(samplePreset));
            when(presetRepository.save(any(Preset.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PresetDetailDTO updated = presetService.updatePresetForUser(10L, "tester@example.com", request);

            assertThat(updated).isNotNull();
            assertThat(updated.id()).isEqualTo(10L);
            assertThat(updated.codigoLinea()).isEqualTo("505");
            assertThat(updated.identificadorParada()).isEqualTo("P-555");
            assertThat(updated.bandera()).isEqualTo("Express");
            assertThat(updated.config()).isEqualTo(updatedConfig);

            verify(presetRepository).save(samplePreset);
            assertThat(samplePreset.getCodigoLinea()).isEqualTo("505");
            assertThat(samplePreset.getIdentificadorParada()).isEqualTo("P-555");
            assertThat(samplePreset.getBandera()).isEqualTo("Express");
            assertThat(samplePreset.getConfig()).isEqualTo(updatedConfig);
        }
    }

    @Nested
    @DisplayName("deletePresetForUser")
    class DeletePresetForUserTests {

        @Test
        @DisplayName("Throws ResourceNotFoundException when user is not found")
        void throwsExceptionWhenUserNotFound() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> presetService.deletePresetForUser(10L, "unknown@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with email: unknown@example.com");

            verify(presetRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when preset is not found")
        void throwsExceptionWhenPresetNotFound() {
            when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(sampleUser));
            when(presetRepository.findByIdAndUserIdWithUser(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> presetService.deletePresetForUser(99L, "tester@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Preset not found with id: 99");

            verify(presetRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Deletes preset from repository when found")
        void deletesPresetSuccessfully() {
            when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(sampleUser));
            when(presetRepository.findByIdAndUserIdWithUser(10L, 1L)).thenReturn(Optional.of(samplePreset));

            presetService.deletePresetForUser(10L, "tester@example.com");

            verify(presetRepository).delete(samplePreset);
        }
    }
}
