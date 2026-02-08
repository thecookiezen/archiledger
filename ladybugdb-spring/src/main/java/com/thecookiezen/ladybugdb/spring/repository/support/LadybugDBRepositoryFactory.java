package com.thecookiezen.ladybugdb.spring.repository.support;

import com.thecookiezen.ladybugdb.spring.core.LadybugDBTemplate;
import com.thecookiezen.ladybugdb.spring.repository.NodeRepository;
import com.thecookiezen.ladybugdb.spring.repository.query.LadybugQueryLookupStrategy;

import java.util.Optional;

import org.springframework.core.ResolvableType;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.ValueExpressionDelegate;

/**
 * Factory for creating LadybugDB repository instances.
 * Detects whether the repository interface extends {@link NodeRepository}
 * or {@link RelationshipRepository} and creates the appropriate implementation.
 */
public class LadybugDBRepositoryFactory extends RepositoryFactorySupport {

    private final LadybugDBTemplate template;
    private final EntityRegistry entityRegistry;

    public LadybugDBRepositoryFactory(LadybugDBTemplate template, EntityRegistry entityRegistry) {
        this.template = template;
        this.entityRegistry = entityRegistry;
    }

    @Override
    public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
        return new LadybugDBEntityInformation<>(domainClass);
    }

    @Override
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(Key key,
            ValueExpressionDelegate valueExpressionDelegate) {
        return Optional.of(new LadybugQueryLookupStrategy(template, entityRegistry));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object getTargetRepository(RepositoryInformation metadata) {
        Class<?> repositoryInterface = metadata.getRepositoryInterface();
        ResolvableType resolvableType = ResolvableType.forClass(repositoryInterface).as(NodeRepository.class);

        Class<?> domainType = metadata.getDomainType();
        Class<?> relationshipType = resolvableType.getGeneric(2).resolve();

        EntityDescriptor<?> descriptor = entityRegistry.getDescriptor(domainType);
        EntityDescriptor<?> relationshipDescriptor = entityRegistry.getDescriptor(relationshipType);

        return new SimpleNodeRepository<>(template,
                (Class<Object>) domainType,
                (Class<Object>) relationshipType,
                (EntityDescriptor<Object>) descriptor,
                (EntityDescriptor<Object>) relationshipDescriptor);
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return SimpleNodeRepository.class;
    }
}
