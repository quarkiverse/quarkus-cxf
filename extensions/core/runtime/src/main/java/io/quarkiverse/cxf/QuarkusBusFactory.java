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

public class QuarkusBusFactory extends CXFBusFactory {

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
        bus.setExtension(new WrapperHelperClassLoader(bus), WrapperHelperCreator.class);
        bus.setExtension(new ExtensionClassLoader(bus), ExtensionClassCreator.class);
        bus.setExtension(new ExceptionClassLoader(bus), ExceptionClassCreator.class);
        bus.setExtension(new WrapperClassLoader(bus), WrapperClassCreator.class);
        bus.setExtension(new FactoryClassLoader(bus), FactoryClassCreator.class);
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

}
