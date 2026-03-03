package com.thecookiezen.ladybugdb.spring.config;

import com.thecookiezen.ladybugdb.spring.repository.support.LadybugDBRepositoryFactoryBean;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.query.QueryLookupStrategy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable LadybugDB repositories.
 * Will scan the package of the annotated configuration class for Spring Data
 * repositories by default.
 * <p>
 * Example usage:
 *
 * <pre>
 * {@literal @}Configuration
 * {@literal @}EnableLadybugDBRepositories(basePackages = "com.example.repositories")
 * public class LadybugDBConfig {
 *     {@literal @}Bean
 *     public LadybugDBTemplate ladybugDBTemplate(LadybugDBConnectionFactory factory) {
 *         return new LadybugDBTemplate(factory);
 *     }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(LadybugDBRepositoriesRegistrar.class)
public @interface EnableLadybugDBRepositories {

    /**
     * Alias for the {@link #basePackages()} attribute.
     */
    String[] value() default {};

    /**
     * Base packages to scan for annotated components.
     */
    String[] basePackages() default {};

    /**
     * Type-safe alternative to {@link #basePackages()} for specifying packages
     * to scan. The package of each class specified will be scanned.
     */
    Class<?>[] basePackageClasses() default {};

    /**
     * Specifies which types are eligible for component scanning.
     */
    ComponentScan.Filter[] includeFilters() default {};

    /**
     * Specifies which types are not eligible for component scanning.
     */
    ComponentScan.Filter[] excludeFilters() default {};

    /**
     * Returns the postfix to be used when looking up custom repository
     * implementations.
     * Defaults to {@literal Impl}.
     */
    String repositoryImplementationPostfix() default "Impl";

    /**
     * Returns the {@link org.springframework.beans.factory.FactoryBean} class to be
     * used for each repository instance.
     */
    Class<?> repositoryFactoryBeanClass() default LadybugDBRepositoryFactoryBean.class;

    /**
     * Configures the name of the
     * {@link com.thecookiezen.ladybugdb.spring.core.LadybugDBTemplate}
     * bean to be used with the repositories detected.
     */
    String ladybugDBTemplateRef() default "ladybugDBTemplate";

    /**
     * Configures whether nested repository interfaces should be detected.
     */
    boolean considerNestedRepositories() default false;

    /**
     * Configures the lookup strategy for query methods.
     */
    QueryLookupStrategy.Key queryLookupStrategy() default QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND;

    /**
     * Configures the location of where to find the Spring Data named queries
     * properties file.
     */
    String namedQueriesLocation() default "";
}
