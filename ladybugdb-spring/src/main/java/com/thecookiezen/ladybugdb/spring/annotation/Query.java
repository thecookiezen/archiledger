package com.thecookiezen.ladybugdb.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.annotation.QueryAnnotation;

@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@QueryAnnotation
public @interface Query {

    /**
     * The actual query string.
     */
    String value() default "";

    /**
     * Whether the query is a "delete" or "update" operation.
     */
    boolean modifying() default false;
}
