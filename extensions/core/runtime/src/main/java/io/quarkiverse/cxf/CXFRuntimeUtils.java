package io.quarkiverse.cxf;

import java.util.Objects;

import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.inject.spi.CDI;

import io.quarkus.arc.Arc;

public class CXFRuntimeUtils {

    /**
     * @param <T> a type to which the returned bean can be casted
     * @param beanRef a fully qualified class name or a name of a {@code @Named} bean prefixed with hash mark ({@code '#'})
     * @param namedBeansSupported if {@code true} then the {@code beanRef} argument may contain a name of a {@code @Named} bean;
     *        otherwise only fully qualified class names can be passed via {@code beanRef}
     * @return an instance of a Bean
     */
    public static <T> T getInstance(String beanRef, boolean namedBeansSupported) {
        if (namedBeansSupported && beanRef != null && beanRef.startsWith("#")) {
            final String beanName = beanRef.substring(1);
            return (T) Arc.container().instance(beanName).get();
        }

        final Class<T> classObj = (Class<T>) loadClass(beanRef);
        Objects.requireNonNull(classObj, "Could not load class " + beanRef);
        try {
            return CDI.current().select(classObj).get();
        } catch (UnsatisfiedResolutionException e) {
            // silent fail
        }
        try {
            return classObj.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not instantiate " + beanRef
                    + " using the default constructor. Make sure that the constructor exists and that the class is static in case it is an inner class.",
                    e);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new RuntimeException("Could not instantiate " + beanRef + " using the default constructor.", e);
        }
    }

    public static <T> T getInstance(String className, String kind, String targetType) {
        try {
            return getInstance(className, true);
        } catch (AmbiguousResolutionException e) {
            /*
             * There are multiple beans of this type
             * and we do not know which one to use
             */
            throw new IllegalStateException("Unable to add a " + kind + " to CXF endpoint " + targetType + ":"
                    + " there are multiple instances of " + className + " available in the CDI container."
                    + " Either make sure there is only one instance available in the container"
                    + " or create a unique subtype of " + className + " and set that one on " + targetType
                    + " or add @jakarta.inject.Named(\"myName\") to some of the beans and refer to that bean by #myName on "
                    + targetType,
                    e);
        }
    }

    private static Class<?> loadClass(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e1) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e2) {
                e1.addSuppressed(e2);
                throw new RuntimeException("Could not load " + className + " using current thread class loader nor "
                        + CXFRuntimeUtils.class.getName() + " class loader", e1);
            }
        }
    }
}
