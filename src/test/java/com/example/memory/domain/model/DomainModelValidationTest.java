package com.example.memory.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelValidationTest {

    @Nested
    @DisplayName("EntityId Validation")
    class EntityIdTest {
        @Test
        void shouldCreateValidEntityId() {
            EntityId id = new EntityId("entity-1");
            assertEquals("entity-1", id.value());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        void shouldThrowExceptionForInvalidEntityId(String invalidValue) {
            Exception exception = assertThrows(IllegalArgumentException.class, () -> new EntityId(invalidValue));
            assertEquals("EntityId cannot be null or blank", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("EntityType Validation")
    class EntityTypeTest {
        @Test
        void shouldCreateValidEntityType() {
            EntityType type = new EntityType("Person");
            assertEquals("Person", type.value());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        void shouldThrowExceptionForInvalidEntityType(String invalidValue) {
            Exception exception = assertThrows(IllegalArgumentException.class, () -> new EntityType(invalidValue));
            assertEquals("EntityType cannot be null or blank", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("RelationType Validation")
    class RelationTypeTest {
        @Test
        void shouldCreateValidRelationType() {
            RelationType type = new RelationType("WORKS_AT");
            assertEquals("WORKS_AT", type.value());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        void shouldThrowExceptionForInvalidRelationType(String invalidValue) {
            Exception exception = assertThrows(IllegalArgumentException.class, () -> new RelationType(invalidValue));
            assertEquals("RelationType cannot be null or blank", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Entity Validation")
    class EntityTest {
        @Test
        void shouldCreateValidEntity() {
            Entity entity = new Entity("name", "type", List.of("obs1"));
            assertEquals("name", entity.name().value());
            assertEquals("type", entity.type().value());
            assertEquals(1, entity.observations().size());
        }

        @Test
        void shouldHandleNullObservations() {
            Entity entity = new Entity(new EntityId("name"), new EntityType("type"), null);
            assertNotNull(entity.observations());
            assertTrue(entity.observations().isEmpty());
        }

        @Test
        void shouldThrowExceptionWhenNameIsNull() {
            assertThrows(IllegalArgumentException.class, () -> new Entity(null, new EntityType("type"), List.of()));
        }

        @Test
        void shouldThrowExceptionWhenTypeIsNull() {
            assertThrows(IllegalArgumentException.class, () -> new Entity(new EntityId("name"), null, List.of()));
        }

        @Test
        void observationsShouldBeImmutable() {
            Entity entity = new Entity("name", "type", List.of("obs1"));
            List<String> obs = entity.observations();
            assertThrows(UnsupportedOperationException.class, () -> obs.add("obs2"));
        }
    }

    @Nested
    @DisplayName("Relation Validation")
    class RelationTest {
        @Test
        void shouldCreateValidRelation() {
            Relation relation = new Relation("from", "to", "relation");
            assertEquals("from", relation.from().value());
            assertEquals("to", relation.to().value());
            assertEquals("relation", relation.relationType().value());
        }

        @Test
        void shouldThrowExceptionWhenFromIsNull() {
            assertThrows(IllegalArgumentException.class, () -> new Relation(null, new EntityId("to"), new RelationType("rel")));
        }

        @Test
        void shouldThrowExceptionWhenToIsNull() {
            assertThrows(IllegalArgumentException.class, () -> new Relation(new EntityId("from"), null, new RelationType("rel")));
        }

        @Test
        void shouldThrowExceptionWhenRelationTypeIsNull() {
            assertThrows(IllegalArgumentException.class, () -> new Relation(new EntityId("from"), new EntityId("to"), null));
        }
    }
}
