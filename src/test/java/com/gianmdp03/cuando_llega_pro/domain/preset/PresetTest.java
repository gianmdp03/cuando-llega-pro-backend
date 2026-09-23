package com.gianmdp03.cuando_llega_pro.domain.preset;

import com.gianmdp03.cuando_llega_pro.domain.preset.model.PresetConfig;
import com.gianmdp03.cuando_llega_pro.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PresetTest {

    @Nested
    @DisplayName("Surrogate Key Identity and Equality")
    class IdentityTests {

        @Test
        @DisplayName("Entities with identical reference should be equal")
        void sameReferenceAreEqual() {
            Preset preset = new Preset("501", "P-100", "A");
            assertThat(preset).isEqualTo(preset);
        }

        @Test
        @DisplayName("Transient entities with null IDs should not be equal to different instances")
        void transientEntitiesAreNotEqual() {
            Preset preset1 = new Preset("501", "P-100", "A");
            Preset preset2 = new Preset("501", "P-100", "A");

            assertThat(preset1).isNotEqualTo(preset2);
        }

        @Test
        @DisplayName("Entities with same ID should be equal")
        void sameIdAreEqual() {
            Preset preset1 = new Preset("501", "P-100", "A");
            Preset preset2 = new Preset("502", "P-200", "B");

            ReflectionTestUtils.setField(preset1, "id", 5L);
            ReflectionTestUtils.setField(preset2, "id", 5L);

            assertThat(preset1).isEqualTo(preset2);
            assertThat(preset1.hashCode()).isEqualTo(preset2.hashCode());
        }

        @Test
        @DisplayName("Entities with different IDs should not be equal")
        void differentIdAreNotEqual() {
            Preset preset1 = new Preset("501", "P-100", "A");
            Preset preset2 = new Preset("501", "P-100", "A");

            ReflectionTestUtils.setField(preset1, "id", 5L);
            ReflectionTestUtils.setField(preset2, "id", 6L);

            assertThat(preset1).isNotEqualTo(preset2);
        }

        @Test
        @DisplayName("Non-Preset object or null should not be equal")
        void nonPresetOrNullNotEqual() {
            Preset preset = new Preset("501", "P-100", "A");
            ReflectionTestUtils.setField(preset, "id", 5L);

            assertThat(preset).isNotEqualTo(null);
            assertThat(preset).isNotEqualTo(new Object());
        }

        @Test
        @DisplayName("HashCode remains stable across identity assignment in a HashSet")
        void hashCodeRemainsStableInHashSet() {
            Set<Preset> set = new HashSet<>();
            Preset preset = new Preset("501", "P-100", "A");

            set.add(preset);
            assertThat(set).contains(preset);

            ReflectionTestUtils.setField(preset, "id", 99L);
            assertThat(set).contains(preset);
        }
    }

    @Nested
    @DisplayName("ToString Safety")
    class ToStringTests {

        @Test
        @DisplayName("toString includes explicit fields and avoids circular user reference")
        void toStringExcludesUserReference() {
            User user = new User("user@example.com", "pass", "User");
            Preset preset = new Preset(user, "501", "P-100", "A");
            ReflectionTestUtils.setField(preset, "id", 15L);

            String str = preset.toString();

            assertThat(str).contains("id=15");
            assertThat(str).contains("codigoLinea=501");
            assertThat(str).contains("identificadorParada=P-100");
            assertThat(str).contains("bandera=A");
            assertThat(str).doesNotContain("user=");
        }
    }

    @Nested
    @DisplayName("Configuration and Lifecycle Callbacks")
    class ConfigAndLifecycleTests {

        @Test
        @DisplayName("Constructor initializes createdAt and stores config")
        void constructorInitializesCreatedAtAndConfig() {
            User user = new User("test@example.com", "pass", "Test User");
            PresetConfig config = new PresetConfig(
                    "Home Stop",
                    "home",
                    "#0055FF",
                    null
            );

            Preset preset = new Preset(user, "511", "001", "A", config);

            assertThat(preset.getCreatedAt()).isNotNull();
            assertThat(preset.getConfig()).isEqualTo(config);
            assertThat(preset.getUser()).isEqualTo(user);
            assertThat(preset.getCodigoLinea()).isEqualTo("511");
            assertThat(preset.getIdentificadorParada()).isEqualTo("001");
            assertThat(preset.getBandera()).isEqualTo("A");
        }

        @Test
        @DisplayName("PrePersist callback sets createdAt when null")
        void prePersistSetsCreatedAtWhenNull() {
            Preset preset = new Preset("511", "001", "A");
            preset.setCreatedAt(null);

            preset.onCreate();

            assertThat(preset.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("PrePersist callback preserves existing createdAt")
        void prePersistPreservesExistingCreatedAt() {
            Preset preset = new Preset("511", "001", "A");
            Instant past = Instant.now().minusSeconds(3600);
            preset.setCreatedAt(past);

            preset.onCreate();

            assertThat(preset.getCreatedAt()).isEqualTo(past);
        }
    }
}
