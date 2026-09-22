package com.gianmdp03.cuando_llega_pro.domain.dashboard;

import com.gianmdp03.cuando_llega_pro.domain.dashboard.dto.DashboardPresetArrivalDTO;
import com.gianmdp03.cuando_llega_pro.domain.dashboard.dto.DashboardResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.preset.Preset;
import com.gianmdp03.cuando_llega_pro.domain.preset.PresetRepository;
import com.gianmdp03.cuando_llega_pro.domain.preset.model.PresetConfig;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.ArrivalResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.BusArrivalItemDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.model.TelemetryStatus;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.service.ArrivalsService;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private PresetRepository presetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ArrivalsService arrivalsService;

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
                new PresetConfig.ScheduleRange("07:00", "08:30", Set.of("MON", "TUE", "WED")),
                new PresetConfig.NotificationSettings(true, 5, true)
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
            verify(arrivalsService, never()).getArrivals(any(), any());
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

            verify(arrivalsService, never()).getArrivals(any(), any());
        }
    }

    @Nested
    @DisplayName("Virtual Thread Fan-Out Across Multiple Presets")
    class VirtualThreadFanOutTests {

        @Test
        @DisplayName("Keeps healthy presets when one upstream request fails")
        void keepsHealthyPresetsWhenOneRequestFails() {
            Preset healthyPreset = new Preset(sampleUser, "501", "P-101", "A", sampleConfig);
            ReflectionTestUtils.setField(healthyPreset, "id", 10L);
            Preset unavailablePreset = new Preset(sampleUser, "502", "P-202", "B", sampleConfig);
            ReflectionTestUtils.setField(unavailablePreset, "id", 20L);

            when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(sampleUser));
            when(presetRepository.findAllByUserIdWithUser(1L))
                    .thenReturn(List.of(healthyPreset, unavailablePreset));
            ArrivalResponseDTO healthyTelemetry = ArrivalResponseDTO.empty("501", "P-101", TelemetryStatus.LIVE);
            when(arrivalsService.getArrivals("501", "P-101")).thenReturn(healthyTelemetry);
            when(arrivalsService.getArrivals("502", "P-202"))
                    .thenThrow(new RuntimeException("upstream unavailable"));

            DashboardResponseDTO response = dashboardService.getDashboardForUser("tester@example.com");

            assertThat(response.presets()).hasSize(2);
            assertThat(response.presets().get(0).telemetry().status()).isEqualTo(TelemetryStatus.LIVE);
            assertThat(response.presets().get(0).telemetry().branch()).isEqualTo("A");
            assertThat(response.presets().get(0).error()).isNull();
            assertThat(response.presets().get(1).telemetry().status()).isEqualTo(TelemetryStatus.UNAVAILABLE);
            assertThat(response.presets().get(1).error()).isEqualTo("Información temporalmente no disponible.");
        }

        @Test
        @DisplayName("Forks tasks on Java 25 Virtual Threads and joins all preset arrival telemetries concurrently")
        void fansOutConcurrentlyUsingVirtualThreads() {
            Preset preset1 = new Preset(sampleUser, "501", "P-101", "A", sampleConfig);
            ReflectionTestUtils.setField(preset1, "id", 10L);

            Preset preset2 = new Preset(sampleUser, "502", "P-202", "B", sampleConfig);
            ReflectionTestUtils.setField(preset2, "id", 20L);

            Preset preset3 = new Preset(sampleUser, "503", "P-303", "C", sampleConfig);
            ReflectionTestUtils.setField(preset3, "id", 30L);

            List<Preset> presets = List.of(preset1, preset2, preset3);

            when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(sampleUser));
            when(presetRepository.findAllByUserIdWithUser(1L)).thenReturn(presets);

            ArrivalResponseDTO telemetry1 = new ArrivalResponseDTO(
                    "501", "P-101", "A", TelemetryStatus.LIVE, Instant.now(), 0L,
                    List.of(new BusArrivalItemDTO("501", "A", 5, 1200, "14:30", "Unit-1", true, TelemetryStatus.LIVE))
            );
            ArrivalResponseDTO telemetry2 = new ArrivalResponseDTO(
                    "502", "P-202", "B", TelemetryStatus.ESTIMATED_FALLBACK, Instant.now(), 3L,
                    List.of(new BusArrivalItemDTO("502", "B", 10, 2500, "14:40", "Unit-2", false, TelemetryStatus.ESTIMATED_FALLBACK))
            );
            ArrivalResponseDTO telemetry3 = new ArrivalResponseDTO(
                    "503", "P-303", "C", TelemetryStatus.LIVE, Instant.now(), 0L,
                    List.of(new BusArrivalItemDTO("503", "C", 2, 400, "14:22", "Unit-3", true, TelemetryStatus.LIVE))
            );

            AtomicBoolean allVirtualThreads = new AtomicBoolean(true);
            ConcurrentLinkedQueue<String> executingThreadNames = new ConcurrentLinkedQueue<>();

            when(arrivalsService.getArrivals("501", "P-101")).thenAnswer(inv -> {
                executingThreadNames.add(Thread.currentThread().getName());
                if (!Thread.currentThread().isVirtual()) {
                    allVirtualThreads.set(false);
                }
                return telemetry1;
            });

            when(arrivalsService.getArrivals("502", "P-202")).thenAnswer(inv -> {
                executingThreadNames.add(Thread.currentThread().getName());
                if (!Thread.currentThread().isVirtual()) {
                    allVirtualThreads.set(false);
                }
                return telemetry2;
            });

            when(arrivalsService.getArrivals("503", "P-303")).thenAnswer(inv -> {
                executingThreadNames.add(Thread.currentThread().getName());
                if (!Thread.currentThread().isVirtual()) {
                    allVirtualThreads.set(false);
                }
                return telemetry3;
            });

            DashboardResponseDTO response = dashboardService.getDashboardForUser("tester@example.com");

            assertThat(response).isNotNull();
            assertThat(response.userEmail()).isEqualTo("tester@example.com");
            assertThat(response.totalPresets()).isEqualTo(3);
            assertThat(response.presets()).hasSize(3);
            assertThat(response.generatedAt()).isNotNull();

            // Verify Java 25 Virtual Thread execution
            assertThat(allVirtualThreads.get()).as("Expected execution on Virtual Threads").isTrue();
            assertThat(executingThreadNames).hasSize(3);

            DashboardPresetArrivalDTO item1 = response.presets().get(0);
            assertThat(item1.presetId()).isEqualTo(10L);
            assertThat(item1.codigoLinea()).isEqualTo("501");
            assertThat(item1.identificadorParada()).isEqualTo("P-101");
            assertThat(item1.bandera()).isEqualTo("A");
            assertThat(item1.config()).isEqualTo(sampleConfig);
            assertThat(item1.telemetry()).isEqualTo(telemetry1);

            DashboardPresetArrivalDTO item2 = response.presets().get(1);
            assertThat(item2.presetId()).isEqualTo(20L);
            assertThat(item2.codigoLinea()).isEqualTo("502");
            assertThat(item2.identificadorParada()).isEqualTo("P-202");
            assertThat(item2.bandera()).isEqualTo("B");
            assertThat(item2.telemetry()).isEqualTo(telemetry2);

            DashboardPresetArrivalDTO item3 = response.presets().get(2);
            assertThat(item3.presetId()).isEqualTo(30L);
            assertThat(item3.codigoLinea()).isEqualTo("503");
            assertThat(item3.identificadorParada()).isEqualTo("P-303");
            assertThat(item3.bandera()).isEqualTo("C");
            assertThat(item3.telemetry()).isEqualTo(telemetry3);

            verify(arrivalsService, times(1)).getArrivals("501", "P-101");
            verify(arrivalsService, times(1)).getArrivals("502", "P-202");
            verify(arrivalsService, times(1)).getArrivals("503", "P-303");
        }
    }

    @Nested
    @DisplayName("Branch Filtering in Dashboard Aggregation")
    class PresetBranchFilteringTests {

        @Test
        @DisplayName("Filters arrivals by preset branch case-insensitively when preset bandera is configured")
        void filtersArrivalsByPresetBranch() {
            Preset preset = new Preset(sampleUser, "511", "P-100", "A", sampleConfig);
            ReflectionTestUtils.setField(preset, "id", 101L);

            when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(sampleUser));
            when(presetRepository.findAllByUserIdWithUser(1L)).thenReturn(List.of(preset));

            BusArrivalItemDTO itemA = new BusArrivalItemDTO("511", "a", 5, 1200, "5 min", "Unit-1", true, TelemetryStatus.LIVE);
            BusArrivalItemDTO itemB = new BusArrivalItemDTO("511", "B", 10, 2500, "10 min", "Unit-2", false, TelemetryStatus.LIVE);

            ArrivalResponseDTO mixedTelemetry = new ArrivalResponseDTO(
                    "511", "P-100", null, TelemetryStatus.LIVE, Instant.now(), 0L,
                    List.of(itemA, itemB)
            );

            when(arrivalsService.getArrivals("511", "P-100")).thenReturn(mixedTelemetry);

            DashboardResponseDTO response = dashboardService.getDashboardForUser("tester@example.com");

            assertThat(response.presets()).hasSize(1);
            DashboardPresetArrivalDTO presetDTO = response.presets().getFirst();
            assertThat(presetDTO.bandera()).isEqualTo("A");
            assertThat(presetDTO.telemetry().arrivals()).hasSize(1);
            assertThat(presetDTO.telemetry().arrivals().getFirst().branch()).isEqualTo("a");
            assertThat(presetDTO.telemetry().arrivals().getFirst().vehicleUnit()).isEqualTo("Unit-1");
        }

        @Test
        @DisplayName("Includes all branches when preset bandera is empty or null")
        void includesAllBranchesWhenPresetBanderaNullOrEmpty() {
            Preset presetNullBandera = new Preset(sampleUser, "511", "P-100", null, sampleConfig);
            ReflectionTestUtils.setField(presetNullBandera, "id", 102L);

            when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(sampleUser));
            when(presetRepository.findAllByUserIdWithUser(1L)).thenReturn(List.of(presetNullBandera));

            BusArrivalItemDTO itemA = new BusArrivalItemDTO("511", "A", 5, 1200, "5 min", "Unit-1", true, TelemetryStatus.LIVE);
            BusArrivalItemDTO itemB = new BusArrivalItemDTO("511", "B", 10, 2500, "10 min", "Unit-2", false, TelemetryStatus.LIVE);

            ArrivalResponseDTO mixedTelemetry = new ArrivalResponseDTO(
                    "511", "P-100", null, TelemetryStatus.LIVE, Instant.now(), 0L,
                    List.of(itemA, itemB)
            );

            when(arrivalsService.getArrivals("511", "P-100")).thenReturn(mixedTelemetry);

            DashboardResponseDTO response = dashboardService.getDashboardForUser("tester@example.com");

            assertThat(response.presets()).hasSize(1);
            DashboardPresetArrivalDTO presetDTO = response.presets().getFirst();
            assertThat(presetDTO.telemetry().arrivals()).hasSize(2);
        }
    }
}
