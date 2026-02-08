package com.thecookiezen.ladybugdb.spring.repository.query;

import java.lang.reflect.Method;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;

import com.thecookiezen.ladybugdb.spring.core.LadybugDBTemplate;
import com.thecookiezen.ladybugdb.spring.repository.support.EntityRegistry;

public class LadybugQueryLookupStrategy implements QueryLookupStrategy {

    private final LadybugDBTemplate template;
    private final EntityRegistry entityRegistry;

    public LadybugQueryLookupStrategy(LadybugDBTemplate template, EntityRegistry entityRegistry) {
        this.template = template;
        this.entityRegistry = entityRegistry;
    }

    @Override
    public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata,
            ProjectionFactory factory, NamedQueries namedQueries) {

        LadybugQueryMethod queryMethod = new LadybugQueryMethod(method, metadata, factory);

        if (queryMethod.hasAnnotatedQuery()) {
            return new LadybugRepositoryQuery(queryMethod, template, entityRegistry);
        }

        throw new UnsupportedOperationException("Query derivation not implemented yet!");
    }

}
