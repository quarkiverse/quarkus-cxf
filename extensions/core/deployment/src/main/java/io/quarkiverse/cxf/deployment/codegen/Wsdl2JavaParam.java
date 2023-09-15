package io.quarkiverse.cxf.deployment.codegen;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

import io.quarkiverse.cxf.deployment.CxfBuildTimeConfig.Wsdl2JavaParameterSet;

/**
 * Maps a method of {@link Wsdl2JavaParameterSet} to command line option of {@code wsdl2java}.
 */
@Qualifier
@Target({ METHOD })
@Retention(RUNTIME)
@Documented
public @interface Wsdl2JavaParam {
    /**
     * The name of a {@code wsdl2java} command line option to which the annotated method should be mapped
     *
     * @return the name of a {@code wsdl2java} command line option to which the annotated method should be mapped
     */
    @Nonbinding
    String value();

    /**
     * The kind of transformer that should be used to map a {@link Wsdl2JavaParameterSet} attribute value to a
     * command line option string
     *
     * @return kind of transformer that should be used to map a {@link Wsdl2JavaParameterSet} attribute value to a
     *         command line option string
     */
    @Nonbinding
    Wsdl2JavaParamTransformer transformer() default Wsdl2JavaParamTransformer.toString;

    /**
     * The kind of collection rendering style.
     *
     * @return the kind of collection rendering style.
     */
    @Nonbinding
    Wsdl2JavaParamCollection collection() default Wsdl2JavaParamCollection.none;

    public enum Wsdl2JavaParamTransformer {
        /** Calls the given type's {@code toString()} on the given {@link Wsdl2JavaParameterSet} attribute value */
        toString,
        /**
         * If the given {@link Wsdl2JavaParameterSet} attribute value is {@code true} passes only the option name without the
         * value; otherwise does not pass anything
         */
        bool;
    }

    public enum Wsdl2JavaParamCollection {
        /** -optionName val1,val2 */
        commaSeparated,
        /** -optionName val1 -optionName val2 */
        multiParam,
        /** Special for {@code -xjc*} family of options */
        xjc,
        /** Used for non-collections */
        none;
    }
}
