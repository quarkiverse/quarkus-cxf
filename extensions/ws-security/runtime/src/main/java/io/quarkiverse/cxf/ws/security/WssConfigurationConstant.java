package io.quarkiverse.cxf.ws.security;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Properties;

import org.apache.wss4j.common.ConfigurationConstants;

import io.quarkiverse.cxf.ws.security.CxfWsSecurityConfig.ClientOrEndpointSecurityConfig;

/**
 * Because {@link ClientOrEndpointSecurityConfig} has so many options that all map to some {@code WSS4J[In|Out]Interceptor}
 * property value, we use this annotation to make the mapping a bit more declarative and less error prone.
 */
@Target({ METHOD })
@Retention(RUNTIME)
public @interface WssConfigurationConstant {
    /**
     * The name of a constant from {@link ConfigurationConstants} to which the annotated method should be mapped.
     *
     * @return the name of a constant from {@link ConfigurationConstants} to which the annotated method should be mapped
     */
    String key() default "";

    /**
     * The kind of transformer that should be used to map the value returned by the annotated method to a value suitable
     * for {@code WSS4J[In|Out]Interceptor} properties.
     *
     * @return kind of transformer that should be used to map a {@link Wsdl2JavaParameterSet} attribute value to a
     *         command line option string
     */
    Transformer transformer() default Transformer.toString;

    public enum Transformer {
        /** Calls the given type's {@code toString()} on the given value */
        toString,
        /** Looks up the given bean reference in the CDI container */
        beanRef,
        /** Makes {@link Properties} out of a Map<String, String> */
        properties,
        /** Calls {@code Integer.parseInt()} on the given type's {@code toString()} */
        toInteger;
    }

}
