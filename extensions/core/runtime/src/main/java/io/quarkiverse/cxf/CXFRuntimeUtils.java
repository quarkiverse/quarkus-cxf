package io.quarkiverse.cxf;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
        return getInstance(classObj);
    }

    /**
     * @param <T> a type to which the returned bean can be casted
     * @param beanClass the type to look up in the CDI container or create via reflection
     * @return an instance of a Bean
     */
    public static <T> T getInstance(Class<? extends T> beanClass) {
        try {
            return CDI.current().select(beanClass).get();
        } catch (UnsatisfiedResolutionException e) {
            // silent fail
        }
        try {
            return beanClass.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not instantiate " + beanClass.getName()
                    + " using the default constructor. Make sure that the constructor exists and that the class is static in case it is an inner class.",
                    e);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new RuntimeException("Could not instantiate " + beanClass.getName() + " using the default constructor.", e);
        }
    }

    public static <T> T getInstance(String beanRef, String beanKind, String sei, String clientOrEndpoint) {
        try {
            return getInstance(beanRef, true);
        } catch (AmbiguousResolutionException e) {
            /*
             * There are multiple beans of this type
             * and we do not know which one to use
             */
            throw new IllegalStateException("Unable to add a " + beanKind + " to CXF " + clientOrEndpoint + " " + sei + ":"
                    + " there are multiple instances of " + beanRef + " available in the CDI container."
                    + " Either make sure there is only one instance available in the container"
                    + " or create a unique subtype of " + beanRef + " and set that one on " + sei
                    + " or add @jakarta.inject.Named(\"myName\") to some of the beans and refer to that bean by #myName on "
                    + sei,
                    e);
        }
    }

    public static <T> T getInstance(Class<? extends T> beanClass, String beanKind, String sei, String clientOrEndpoint) {
        try {
            return getInstance(beanClass);
        } catch (AmbiguousResolutionException e) {
            /*
             * There are multiple beans of this type
             * and we do not know which one to use
             */
            throw new IllegalStateException("Unable to add a " + beanKind + " to CXF " + clientOrEndpoint + " " + sei + ":"
                    + " there are multiple instances of " + beanClass.getName() + " available in the CDI container."
                    + " Either make sure there is only one instance available in the container"
                    + " or create a unique subtype of " + beanClass.getName() + " and set that one on " + sei
                    + " or add @jakarta.inject.Named(\"myName\") to some of the beans and refer to that bean by #myName on "
                    + sei,
                    e);
        }
    }

    public static <T> void addBeans(List<String> beanRefs, String beanKind, String sei, String clientOrEndpoint,
            List<T> destination) {
        for (String beanRef : beanRefs) {
            T item = getInstance(beanRef, beanKind, sei, clientOrEndpoint);
            if (item == null) {
                throw new IllegalStateException("Could not lookup bean " + beanRef);
            } else {
                destination.add(item);
            }
        }
    }

    public static <T> void addBeansByType(List<Class<? extends T>> beanTypes, String beanKind, String sei,
            String clientOrEndpoint,
            List<T> destination) {
        for (Class<? extends T> beanType : beanTypes) {
            T item = getInstance(beanType, beanKind, sei, clientOrEndpoint);
            if (item == null) {
                throw new IllegalStateException("Could not lookup bean " + beanType.getName());
            } else {
                destination.add(item);
            }
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

    public static InputStream openStream(final String keystorePath) throws IOException {
        final URL url = Thread.currentThread().getContextClassLoader().getResource(keystorePath);
        if (url != null) {
            try {
                return url.openStream();
            } catch (IOException e) {
                throw new RuntimeException("Could not open " + keystorePath + " from the class path", e);
            }
        }
        final Path path = Path.of(keystorePath);
        if (Files.exists(path)) {
            try {
                return Files.newInputStream(path);
            } catch (IOException e) {
                throw new RuntimeException("Could not open " + keystorePath + " from the filesystem", e);
            }
        }
        final String msg = "Resource " + keystorePath + " exists neither in class path nor in the filesystem";
        QuarkusHTTPConduitFactory.log.error(msg);
        throw new IllegalStateException(msg);
    }

    public static byte[] read(final String keystorePath) {
        final URL url = Thread.currentThread().getContextClassLoader().getResource(keystorePath);
        if (url != null) {
            try (InputStream in = url.openStream()) {
                return in.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException("Could not open " + keystorePath + " from the class path", e);
            }
        }
        final Path path = Path.of(keystorePath);
        if (Files.exists(path)) {
            try {
                return Files.readAllBytes(path);
            } catch (IOException e) {
                throw new RuntimeException("Could not open " + keystorePath + " from the filesystem", e);
            }
        }
        final String msg = "Resource " + keystorePath + " exists neither in class path nor in the filesystem";
        QuarkusHTTPConduitFactory.log.error(msg);
        throw new IllegalStateException(msg);
    }
}
