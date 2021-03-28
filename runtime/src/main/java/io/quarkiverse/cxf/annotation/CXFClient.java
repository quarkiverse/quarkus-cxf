package io.quarkiverse.cxf.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

/**
 * CXFClient documentation.
 */
@Qualifier
@Target({ METHOD, CONSTRUCTOR, FIELD })
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
