package io.quarkiverse.cxf.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Marks a producer method as an endpoint to expose under the path (relative to {@code quarkus.cxf.path} specified in
 * {@link #value()}.
 *
 * @since 3.11.0
 */
@Qualifier
@Target({ METHOD, TYPE })
@Retention(RUNTIME)
@Documented
public @interface CXFEndpoint {
    /**
     * A path relative to {@code quarkus.cxf.path} under which this endpoint should be exposed.
     *
     * @return path relative to {@code quarkus.cxf.path} under which this endpoint should be exposed.
     */
    String value();

    @SuppressWarnings("serial")
    class CXFEndpointLiteral extends AnnotationLiteral<CXFEndpoint> implements CXFEndpoint {

        private final String value;

        public CXFEndpointLiteral(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

}
