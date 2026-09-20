package com.gianmdp03.cuando_llega_pro.domain.user;

import com.gianmdp03.cuando_llega_pro.domain.preset.Preset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Nested
    @DisplayName("Surrogate Key Identity and Equality")
    class IdentityTests {

        @Test
        @DisplayName("Entities with identical reference should be equal")
        void sameReferenceAreEqual() {
            User user = new User("test@example.com", "secret123", "Test User");
            assertThat(user).isEqualTo(user);
        }

        @Test
        @DisplayName("Entities with null IDs should not be equal to different instances")
        void transientEntitiesAreNotEqual() {
            User user1 = new User("test1@example.com", "secret123", "User 1");
            User user2 = new User("test2@example.com", "secret123", "User 2");

            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        @DisplayName("Entities with same ID should be equal regardless of other fields")
        void sameIdAreEqual() {
            User user1 = new User("test1@example.com", "secret1", "User 1");
            User user2 = new User("test2@example.com", "secret2", "User 2");

            ReflectionTestUtils.setField(user1, "id", 1L);
            ReflectionTestUtils.setField(user2, "id", 1L);

            assertThat(user1).isEqualTo(user2);
            assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        }

        @Test
        @DisplayName("Entities with different IDs should not be equal")
        void differentIdAreNotEqual() {
            User user1 = new User("test@example.com", "secret", "User");
            User user2 = new User("test@example.com", "secret", "User");

            ReflectionTestUtils.setField(user1, "id", 1L);
            ReflectionTestUtils.setField(user2, "id", 2L);

            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        @DisplayName("Non-User object or null should not be equal")
        void nonUserOrNullNotEqual() {
            User user = new User("test@example.com", "secret", "User");
            ReflectionTestUtils.setField(user, "id", 1L);

            assertThat(user).isNotEqualTo(null);
            assertThat(user).isNotEqualTo("some string");
        }

        @Test
        @DisplayName("HashCode remains stable across identity assignment in a HashSet")
        void hashCodeRemainsStableInHashSet() {
            Set<User> set = new HashSet<>();
            User user = new User("test@example.com", "secret", "User");

            set.add(user);
            assertThat(set).contains(user);

            // Simulate persistence by setting ID
            ReflectionTestUtils.setField(user, "id", 42L);

            assertThat(set).contains(user);
        }
    }

    @Nested
    @DisplayName("Preset Association Management")
    class PresetAssociationTests {

        @Test
        @DisplayName("addPreset should add to set and establish bidirectional reference")
        void addPresetMaintainsBidirectionalLink() {
            User user = new User("test@example.com", "secret", "User");
            Preset preset = new Preset("501", "P-100", "A");

            user.addPreset(preset);

            assertThat(user.getPresets()).contains(preset);
            assertThat(preset.getUser()).isSameAs(user);
        }

        @Test
        @DisplayName("removePreset should remove from set and nullify user reference")
        void removePresetClearsBidirectionalLink() {
            User user = new User("test@example.com", "secret", "User");
            Preset preset = new Preset("501", "P-100", "A");

            user.addPreset(preset);
            user.removePreset(preset);

            assertThat(user.getPresets()).doesNotContain(preset);
            assertThat(preset.getUser()).isNull();
        }
    }

    @Nested
    @DisplayName("Lifecycle Callbacks and Defaults")
    class LifecycleTests {

        @Test
        @DisplayName("onCreate should set default role and createdAt if null")
        void onCreateSetsDefaults() {
            User user = new User();
            assertThat(user.getRole()).isNull();
            assertThat(user.getCreatedAt()).isNull();

            user.onCreate();

            assertThat(user.getRole()).isEqualTo("ROLE_USER");
            assertThat(user.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("onCreate preserves existing role and createdAt")
        void onCreatePreservesExisting() {
            User user = new User();
            Instant existingTime = Instant.now().minusSeconds(100);
            user.setRole("ROLE_ADMIN");
            user.setCreatedAt(existingTime);

            user.onCreate();

            assertThat(user.getRole()).isEqualTo("ROLE_ADMIN");
            assertThat(user.getCreatedAt()).isEqualTo(existingTime);
        }
    }

    @Nested
    @DisplayName("ToString Safety")
    class ToStringTests {

        @Test
        @DisplayName("toString should not include password or presets")
        void toStringExcludesSensitiveFields() {
            User user = new User("test@example.com", "super_secret_password", "Jane Doe");
            ReflectionTestUtils.setField(user, "id", 10L);

            String str = user.toString();

            assertThat(str).contains("id=10");
            assertThat(str).contains("email=test@example.com");
            assertThat(str).contains("fullName=Jane Doe");
            assertThat(str).doesNotContain("super_secret_password");
            assertThat(str).doesNotContain("presets");
        }
    }
}
