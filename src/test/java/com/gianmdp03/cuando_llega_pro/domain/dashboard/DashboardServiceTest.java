package com.gianmdp03.cuando_llega_pro.domain.dashboard;

import com.gianmdp03.cuando_llega_pro.domain.dashboard.dto.DashboardPresetArrivalDTO;
import com.gianmdp03.cuando_llega_pro.domain.dashboard.dto.DashboardResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.preset.Preset;
import com.gianmdp03.cuando_llega_pro.domain.preset.PresetRepository;
import com.gianmdp03.cuando_llega_pro.domain.preset.model.PresetConfig;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.model.TelemetryStatus;
import com.gianmdp03.cuando_llega_pro.domain.user.User;
import com.gianmdp03.cuando_llega_pro.domain.user.UserRepository;
import com.gianmdp03.cuando_llega_pro.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private PresetRepository presetRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private User sampleUser;
    private PresetConfig sampleConfig;

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
    }

    @Nested
    @DisplayName("User Resolution & Validation")
    class UserValidationTests {

        @Test
        @DisplayName("Throws ResourceNotFoundException when user email does not exist")
        void throwsExceptionWhenUserNotFound() {
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dashboardService.getDashboardForUser("nonexistent@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with email: nonexistent@example.com");

            verify(presetRepository, never()).findAllByUserIdWithUser(any());
        }
    }

    @Nested
    @DisplayName("Empty Presets Handling")
    class EmptyPresetsTests {

        @Test
        @DisplayName("Returns empty DashboardResponseDTO when user has configured zero presets")
        void returnsEmptyDashboardWhenNoPresets() {
            when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(sampleUser));
            when(presetRepository.findAllByUserIdWithUser(1L)).thenReturn(List.of());

            DashboardResponseDTO response = dashboardService.getDashboardForUser("tester@example.com");

            assertThat(response).isNotNull();
            assertThat(response.userEmail()).isEqualTo("tester@example.com");
            assertThat(response.totalPresets()).isZero();
            assertThat(response.presets()).isEmpty();
            assertThat(response.generatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Telemetry Delegation to Client Worker")
    class TelemetryDelegationTests {

        @Test
        @DisplayName("Returns presets with DELEGATED_TO_CLIENT status without calling upstream arrivals")
        void returnsPresetsWithDelegatedTelemetryWithoutUpstreamCalls() {
            Preset preset1 = new Preset(sampleUser, "501", "P-101", "A", sampleConfig);
            ReflectionTestUtils.setField(preset1, "id", 10L);

            Preset preset2 = new Preset(sampleUser, "502", "P-202", "B", sampleConfig);
            ReflectionTestUtils.setField(preset2, "id", 20L);

            when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(sampleUser));
            when(presetRepository.findAllByUserIdWithUser(1L)).thenReturn(List.of(preset1, preset2));

            DashboardResponseDTO response = dashboardService.getDashboardForUser("tester@example.com");

            assertThat(response).isNotNull();
            assertThat(response.userEmail()).isEqualTo("tester@example.com");
            assertThat(response.totalPresets()).isEqualTo(2);
            assertThat(response.presets()).hasSize(2);

            DashboardPresetArrivalDTO item1 = response.presets().get(0);
            assertThat(item1.presetId()).isEqualTo(10L);
            assertThat(item1.codigoLinea()).isEqualTo("501");
            assertThat(item1.identificadorParada()).isEqualTo("P-101");
            assertThat(item1.bandera()).isEqualTo("A");
            assertThat(item1.telemetry().status()).isEqualTo(TelemetryStatus.DELEGATED_TO_CLIENT);
            assertThat(item1.telemetry().arrivals()).isEmpty();
            assertThat(item1.error()).isNull();

            DashboardPresetArrivalDTO item2 = response.presets().get(1);
            assertThat(item2.presetId()).isEqualTo(20L);
            assertThat(item2.codigoLinea()).isEqualTo("502");
            assertThat(item2.identificadorParada()).isEqualTo("P-202");
            assertThat(item2.bandera()).isEqualTo("B");
            assertThat(item2.telemetry().status()).isEqualTo(TelemetryStatus.DELEGATED_TO_CLIENT);
            assertThat(item2.telemetry().arrivals()).isEmpty();
            assertThat(item2.error()).isNull();
        }
    }
}
