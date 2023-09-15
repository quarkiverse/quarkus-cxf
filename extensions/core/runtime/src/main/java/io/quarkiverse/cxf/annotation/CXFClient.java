package io.quarkiverse.cxf.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

/**
 * Marks an injection point for injecting a WebService client.
 */
@Qualifier
@Target({ FIELD, PARAMETER, METHOD })
@Retention(RUNTIME)
@Documented
public @interface CXFClient {
    /**
     * The client key, such as {@code myClient} present in application configuration, e.g. {@code quarkus.cxf.client.myClient}.
     *
     * @return the client key, such as {@code myClient} present in application configuration, e.g.
     *         {@code quarkus.cxf.client.myClient}
     */
    @Nonbinding
    String value() default "";
}
