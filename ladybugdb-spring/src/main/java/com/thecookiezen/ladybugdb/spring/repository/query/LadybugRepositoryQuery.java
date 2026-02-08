package com.thecookiezen.ladybugdb.spring.repository.query;

import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

import com.thecookiezen.ladybugdb.spring.core.LadybugDBTemplate;
import com.thecookiezen.ladybugdb.spring.mapper.RowMapper;
import com.thecookiezen.ladybugdb.spring.mapper.ValueMappers;
import com.thecookiezen.ladybugdb.spring.repository.support.EntityDescriptor;
import com.thecookiezen.ladybugdb.spring.repository.support.EntityRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LadybugRepositoryQuery implements RepositoryQuery {

    private final LadybugQueryMethod queryMethod;
    private final LadybugDBTemplate template;
    private final EntityRegistry entityRegistry;

    public LadybugRepositoryQuery(LadybugQueryMethod method, LadybugDBTemplate template,
            EntityRegistry entityRegistry) {
        this.queryMethod = method;
        this.template = template;
        this.entityRegistry = entityRegistry;
    }

    @Override
    public Object execute(Object[] parameters) {
        String queryString = queryMethod.getAnnotatedQuery();
        Class<?> domainType = queryMethod.getReturnedObjectType();

        Map<String, Object> params = new HashMap<>();
        for (Parameter param : queryMethod.getParameters()) {
            if (param.isBindable()) {
                String paramName = param.getName().orElse("arg" + param.getIndex());
                Object value = parameters[param.getIndex()];
                params.put(paramName, value);
            }
        }

        RowMapper<?> mapper = getRowMapper(domainType);

        @SuppressWarnings("unchecked")
        List<?> results = template.query(queryString, params, (RowMapper<Object>) mapper);

        if (queryMethod.isCollectionQuery()) {
            return results;
        } else {
            return results.isEmpty() ? null : results.get(0);
        }
    }

    private RowMapper<?> getRowMapper(Class<?> domainType) {
        EntityDescriptor<?> descriptor = entityRegistry.getDescriptor(domainType);
        if (descriptor != null) {
            return descriptor.reader();
        }
        if (String.class.equals(domainType)) {
            return row -> ValueMappers.asString(row.getValue(0));
        }
        if (Integer.class.equals(domainType)) {
            return row -> ValueMappers.asInteger(row.getValue(0));
        }
        if (Long.class.equals(domainType)) {
            return row -> ValueMappers.asLong(row.getValue(0));
        }
        if (Double.class.equals(domainType)) {
            return row -> ValueMappers.asDouble(row.getValue(0));
        }
        if (Boolean.class.equals(domainType)) {
            return row -> ValueMappers.asBoolean(row.getValue(0));
        }
        throw new IllegalArgumentException("No RowMapper found for type " + domainType.getName());
    }

    @Override
    public QueryMethod getQueryMethod() {
        return queryMethod;
    }

}
