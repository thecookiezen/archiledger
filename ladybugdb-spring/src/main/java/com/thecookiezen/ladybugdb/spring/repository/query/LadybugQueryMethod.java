package com.thecookiezen.ladybugdb.spring.repository.query;

import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.DefaultParameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.util.StringUtils;

import com.thecookiezen.ladybugdb.spring.annotation.Query;

public class LadybugQueryMethod extends QueryMethod {
    private final Method method;

    public LadybugQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
        super(method, metadata, factory, (parametersSource) -> new DefaultParameters(parametersSource));
        this.method = method;
    }

    public boolean hasAnnotatedQuery() {
        return getAnnotatedQuery() != null;
    }

    public String getAnnotatedQuery() {
        Query query = AnnotationUtils.findAnnotation(method, Query.class);
        if (query != null && StringUtils.hasText(query.value())) {
            return query.value();
        }
        return null;
    }
}
