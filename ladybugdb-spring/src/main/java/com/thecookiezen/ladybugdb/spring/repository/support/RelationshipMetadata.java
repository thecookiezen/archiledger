package com.thecookiezen.ladybugdb.spring.repository.support;

import com.thecookiezen.ladybugdb.spring.annotation.RelationshipEntity;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * Metadata about a relationship entity type, including type name
 * and source/target references.
 *
 * @param <R> the relationship entity type
 */
public class RelationshipMetadata<R> {

    private final Class<R> relationshipType;
    private final String relationshipTypeName;
    private final Class<?> sourceType;
    private final Class<?> targetType;
    private final String sourceFieldName;
    private final String targetFieldName;
    private final Field idField;
    private final String idPropertyName;
    private final List<String> propertyNames;

    public RelationshipMetadata(Class<R> relationshipType) {
        this.relationshipType = relationshipType;
        this.relationshipTypeName = determineTypeName(relationshipType);

        RelationshipEntity annotation = relationshipType.getAnnotation(RelationshipEntity.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Relationship entity must be annotated with @RelationshipEntity");
        }

        this.sourceType = annotation.nodeType();
        this.targetType = annotation.nodeType();
        this.sourceFieldName = annotation.sourceField();
        this.targetFieldName = annotation.targetField();
        this.idField = findIdField(relationshipType);
        this.idPropertyName = idField != null ? idField.getName() : "id";
        this.propertyNames = extractPropertyNames(relationshipType);
    }

    private List<String> extractPropertyNames(Class<R> relationshipType) {
        return Arrays.stream(relationshipType.getDeclaredFields())
                .map(Field::getName)
                .toList();
    }

    private Field findIdField(Class<R> relationshipType) {
        for (Field field : relationshipType.getDeclaredFields()) {
            for (Annotation ann : field.getAnnotations()) {
                String annotationName = ann.annotationType().getSimpleName();
                if ("Id".equals(annotationName)) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        throw new IllegalArgumentException("Relationship entity must have an @Id field");
    }

    private String determineTypeName(Class<R> relationshipType) {
        RelationshipEntity annotation = relationshipType.getAnnotation(RelationshipEntity.class);
        if (annotation != null && !annotation.type().isEmpty()) {
            return annotation.type();
        }

        String className = relationshipType.getSimpleName();
        if (className.endsWith("Relationship")) {
            className = className.substring(0, className.length() - 12);
        } else if (className.endsWith("Rel")) {
            className = className.substring(0, className.length() - 3);
        }
        return className;
    }

    public Class<R> getRelationshipType() {
        return relationshipType;
    }

    public String getRelationshipTypeName() {
        return relationshipTypeName;
    }

    public Class<?> getSourceType() {
        return sourceType;
    }

    public Class<?> getTargetType() {
        return targetType;
    }

    public String getSourceFieldName() {
        return sourceFieldName;
    }

    public String getTargetFieldName() {
        return targetFieldName;
    }

    public String getIdPropertyName() {
        return idPropertyName;
    }

    public List<String> getPropertyNames() {
        return propertyNames;
    }

    @SuppressWarnings("unchecked")
    public <ID> ID getId(R entity) {
        if (idField == null) {
            return null;
        }
        try {
            return (ID) idField.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access ID field", e);
        }
    }
}
