package com.thecookiezen.ladybugdb.spring.repository.support;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;

import static org.junit.jupiter.api.Assertions.*;

class LadybugDBEntityInformationTest {

    @Test
    void isNew_shouldReturnTrueWhenIdIsNull() {
        LadybugDBEntityInformation<TestEntity, String> info = new LadybugDBEntityInformation<>(TestEntity.class);
        TestEntity entity = new TestEntity();
        entity.id = null;

        assertTrue(info.isNew(entity));
    }

    @Test
    void isNew_shouldReturnFalseWhenIdIsNotNull() {
        LadybugDBEntityInformation<TestEntity, String> info = new LadybugDBEntityInformation<>(TestEntity.class);
        TestEntity entity = new TestEntity();
        entity.id = "existing-id";

        assertFalse(info.isNew(entity));
    }

    @Test
    void getId_shouldReturnEntityId() {
        LadybugDBEntityInformation<TestEntity, String> info = new LadybugDBEntityInformation<>(TestEntity.class);
        TestEntity entity = new TestEntity();
        entity.id = "test-123";

        assertEquals("test-123", info.getId(entity));
    }

    @Test
    void getIdType_shouldReturnIdFieldType() {
        LadybugDBEntityInformation<TestEntity, String> info = new LadybugDBEntityInformation<>(TestEntity.class);

        assertEquals(String.class, info.getIdType());
    }

    @Test
    void getIdType_shouldReturnObjectWhenNoIdField() {
        LadybugDBEntityInformation<EntityWithoutId, Object> info = new LadybugDBEntityInformation<>(
                EntityWithoutId.class);

        assertEquals(Object.class, info.getIdType());
    }

    @Test
    void getJavaType_shouldReturnEntityClass() {
        LadybugDBEntityInformation<TestEntity, String> info = new LadybugDBEntityInformation<>(TestEntity.class);

        assertEquals(TestEntity.class, info.getJavaType());
    }

    static class TestEntity {
        @Id
        String id;
        String name;
    }

    static class EntityWithoutId {
        String name;
    }
}
