package io.quarkiverse.cxf.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

@Documented
@Retention(RUNTIME)
public @interface CXF {
    /**
     * The name.
     * 
     * @return the name.
     */
    String config() default "";
}
