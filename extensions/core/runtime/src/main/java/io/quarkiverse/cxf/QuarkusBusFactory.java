package io.quarkiverse.cxf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.common.spi.ClassLoaderProxyService;
import org.apache.cxf.common.spi.ClassLoaderService;
import org.apache.cxf.common.spi.GeneratedNamespaceClassLoader;
import org.apache.cxf.common.spi.NamespaceClassCreator;
import org.apache.cxf.endpoint.dynamic.ExceptionClassCreator;
import org.apache.cxf.endpoint.dynamic.ExceptionClassLoader;
import org.apache.cxf.jaxb.FactoryClassCreator;
import org.apache.cxf.jaxb.FactoryClassLoader;
import org.apache.cxf.jaxb.WrapperHelperClassLoader;
import org.apache.cxf.jaxb.WrapperHelperCreator;
import org.apache.cxf.jaxws.spi.WrapperClassCreator;
import org.apache.cxf.jaxws.spi.WrapperClassLoader;
import org.apache.cxf.wsdl.ExtensionClassCreator;
import org.apache.cxf.wsdl.ExtensionClassLoader;
import org.jboss.logging.Logger;

public class QuarkusBusFactory extends CXFBusFactory {

    private static final Logger log = Logger.getLogger(QuarkusBusFactory.class);

    /** {@link List} of customizers passed via {@code RuntimeBusCustomizerBuildItem} */
    private static final List<Consumer<Bus>> customizers = new CopyOnWriteArrayList<>();

    @Override
    public Bus createBus(Map<Class<?>, Object> extensions, Map<String, Object> properties) {
        if (extensions == null) {
            extensions = new HashMap<Class<?>, Object>();
        }
        final Bus bus = super.createBus(extensions, properties);
        for (Consumer<Bus> customizer : customizers) {
            customizer.accept(bus);
        }

        bus.setExtension(new QuarkusWrapperHelperClassLoader(bus), WrapperHelperCreator.class);
        bus.setExtension(new QuarkusExtensionClassLoader(bus), ExtensionClassCreator.class);
        bus.setExtension(new QuarkusExceptionClassLoader(bus), ExceptionClassCreator.class);
        bus.setExtension(new QuarkusWrapperClassLoader(bus), WrapperClassCreator.class);
        bus.setExtension(new QuarkusFactoryClassLoader(bus), FactoryClassCreator.class);
        bus.setExtension(new GeneratedNamespaceClassLoader(bus), NamespaceClassCreator.class);
        bus.setExtension(new ClassLoaderProxyService(new GeneratedNamespaceClassLoader(bus)), ClassLoaderService.class);
        return bus;
    }

    /**
     * @param customizer a {@link Consumer} to run right after the creation of the runtime {@link Bus}
     */
    static void addBusCustomizer(Consumer<Bus> customizer) {
        customizers.add(customizer);
    }

    public static class QuarkusWrapperHelperClassLoader extends WrapperHelperClassLoader {

        public QuarkusWrapperHelperClassLoader(Bus bus) {
            super(bus);
        }

        @Override
        protected Class<?> findClass(String className, Class<?> callingClass) {
            return loadClass(className);
        }
    }

    public static class QuarkusExtensionClassLoader extends ExtensionClassLoader {

        public QuarkusExtensionClassLoader(Bus bus) {
            super(bus);
        }

        @Override
        protected Class<?> findClass(String className, Class<?> callingClass) {
            return loadClass(className);
        }
    }

    public static class QuarkusExceptionClassLoader extends ExceptionClassLoader {

        public QuarkusExceptionClassLoader(Bus bus) {
            super(bus);
        }

        @Override
        protected Class<?> findClass(String className, Class<?> callingClass) {
            return loadClass(className);
        }
    }

    public static class QuarkusWrapperClassLoader extends WrapperClassLoader {
        public QuarkusWrapperClassLoader(Bus bus) {
            super(bus);
        }

        @Override
        protected Class<?> findClass(String className, Class<?> callingClass) {
            return loadClass(className);
        }
    }

    public static class QuarkusFactoryClassLoader extends FactoryClassLoader {
        public QuarkusFactoryClassLoader(Bus bus) {
            super(bus);
        }

        @Override
        protected Class<?> findClass(String className, Class<?> callingClass) {
            return loadClass(className);
        }
    }

    static Class<?> loadClass(String className) {
        try {
            return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load " + className, e);
        }
    }

}
