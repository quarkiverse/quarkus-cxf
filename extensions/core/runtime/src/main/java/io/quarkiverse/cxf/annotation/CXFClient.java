package io.quarkiverse.cxf.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

/**
 * CXFClient documentation.
 */
@Qualifier
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
@Documented
public @interface CXFClient {
    /**
     * The name.
     *
     * @return the name.
     */
    @Nonbinding
    String value() default "";
}
