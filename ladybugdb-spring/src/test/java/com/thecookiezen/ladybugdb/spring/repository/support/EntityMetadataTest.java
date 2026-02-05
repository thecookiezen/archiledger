package com.thecookiezen.ladybugdb.spring.repository.support;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;

import static org.junit.jupiter.api.Assertions.*;

class EntityMetadataTest {

    @Nested
    class IdFieldDetection {

        @Test
        void shouldFindSpringDataIdAnnotation() {
            EntityMetadata<EntityWithSpringId> metadata = new EntityMetadata<>(EntityWithSpringId.class);

            assertNotNull(metadata.getIdField());
            assertEquals("id", metadata.getIdField().getName());
        }

        @Test
        void shouldReturnNullWhenNoIdField() {
            EntityMetadata<EntityWithoutId> metadata = new EntityMetadata<>(EntityWithoutId.class);

            assertNull(metadata.getIdField());
        }
    }

    @Nested
    class NodeLabelDerivation {

        @Test
        void shouldDeriveSimpleClassName() {
            EntityMetadata<Person> metadata = new EntityMetadata<>(Person.class);

            assertEquals("Person", metadata.getNodeLabel());
        }

        @Test
        void shouldRemoveEntitySuffix() {
            EntityMetadata<UserEntity> metadata = new EntityMetadata<>(UserEntity.class);

            assertEquals("User", metadata.getNodeLabel());
        }

        @Test
        void shouldHandleEntityOnlyName() {
            EntityMetadata<Entity> metadata = new EntityMetadata<>(Entity.class);

            assertEquals("Entity", metadata.getNodeLabel());
        }
    }

    @Nested
    class IdExtraction {

        @Test
        void shouldExtractIdValue() {
            EntityMetadata<EntityWithSpringId> metadata = new EntityMetadata<>(EntityWithSpringId.class);
            EntityWithSpringId entity = new EntityWithSpringId();
            entity.id = "test-123";

            Object id = metadata.getId(entity);

            assertEquals("test-123", id);
        }

        @Test
        void shouldReturnNullForNullId() {
            EntityMetadata<EntityWithSpringId> metadata = new EntityMetadata<>(EntityWithSpringId.class);
            EntityWithSpringId entity = new EntityWithSpringId();
            entity.id = null;

            Object id = metadata.getId(entity);

            assertNull(id);
        }

        @Test
        void shouldReturnNullWhenNoIdField() {
            EntityMetadata<EntityWithoutId> metadata = new EntityMetadata<>(EntityWithoutId.class);
            EntityWithoutId entity = new EntityWithoutId();

            Object id = metadata.getId(entity);

            assertNull(id);
        }
    }

    @Nested
    class EntityType {

        @Test
        void shouldReturnCorrectEntityType() {
            EntityMetadata<Person> metadata = new EntityMetadata<>(Person.class);

            assertEquals(Person.class, metadata.getEntityType());
        }
    }

    static class EntityWithSpringId {
        @Id
        String id;
        String name;
    }

    static class EntityWithoutId {
        String name;
        int value;
    }

    static class Person {
        @Id
        String id;
    }

    static class UserEntity {
        @Id
        String id;
    }

    static class Entity {
        @Id
        String id;
    }
}
