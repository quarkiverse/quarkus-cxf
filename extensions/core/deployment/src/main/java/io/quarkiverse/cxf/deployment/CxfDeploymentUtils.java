package io.quarkiverse.cxf.deployment;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.cxf.Bus;
import org.apache.cxf.common.spi.GeneratedClassClassLoaderCapture;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsClientFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public class CxfDeploymentUtils {

    private CxfDeploymentUtils() {
    }

    public static Stream<AnnotationInstance> methodsWithCxfEndpointAnnotations(IndexView index) {
        return Stream.of(CxfDotNames.CXF_ENDPOINT_ANNOTATION)
                .map(index::getAnnotations)
                .flatMap(Collection::stream)
                .filter(annotation -> annotation.target().kind() == AnnotationTarget.Kind.METHOD);
    }

    public static Stream<AnnotationInstance> webServiceAnnotations(IndexView index) {
        return Stream.of(CxfDotNames.WEBSERVICE_ANNOTATION, CxfDotNames.WEBSERVICE_PROVIDER_ANNOTATION)
                .map(index::getAnnotations)
                .flatMap(Collection::stream)
                .filter(annotation -> annotation.target().kind() == AnnotationTarget.Kind.CLASS);
    }

    public static String findSei(IndexView index, ClassInfo wsClassInfo) {

        {
            final AnnotationInstance webServiceAnnotation = wsClassInfo.annotation(CxfDotNames.WEBSERVICE_ANNOTATION);
            if (webServiceAnnotation != null) {
                if (wsClassInfo.isInterface()) {
                    return wsClassInfo.name().toString();
                }
                final AnnotationValue val = webServiceAnnotation.value("endpointInterface");
                if (val != null) {
                    final String result = val.asString();
                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            }
        }

        final List<DotName> interfaces = wsClassInfo.interfaceNames();
        for (DotName interfaceName : interfaces) {
            final ClassInfo interfaceInfo = index.getClassByName(interfaceName);
            if (interfaceInfo == null) {
                throw new IllegalStateException("Cannot find " + interfaceName + " in Quarkus class index (Jandex)."
                        + " You may want to register the artifact containing " + interfaceName
                        + " for indexing using quarkus.index-dependency.* family of properties"
                        + " or you may want to add io.smallrye:jandex-maven-plugin to the module containing sources of "
                        + interfaceName
                        + ". See https://quarkus.io/guides/cdi-reference#bean_discovery for more information.");
            }
            final AnnotationInstance webServiceAnnotation = interfaceInfo.annotation(CxfDotNames.WEBSERVICE_ANNOTATION);
            if (webServiceAnnotation != null) {
                return interfaceInfo.name().toString();
            }
        }
        return wsClassInfo.name().toString();
    }

    /**
     * Creates a server at runtime thus triggering the generation of all required classes. This includes
     * CXF extensions, endpoint wrappers, etc.
     *
     * @param sei the fully qualified name of the service class for which the server should be created
     * @param bus the bus to use with {@link GeneratedClassClassLoaderCapture} set properly.
     */
    public static void createServer(String sei, String path, Bus bus) {
        JaxWsServerFactoryBean factoryBean = new JaxWsServerFactoryBean();
        factoryBean.setBus(bus);
        try {
            factoryBean.setServiceClass(Thread.currentThread().getContextClassLoader().loadClass(sei));
            factoryBean.setAddress(path);
            Server server = factoryBean.create();
            server.destroy();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load " + sei, e);
        }
    }

    /**
     * Creates a client at runtime thus triggering the generation of all required classes. This includes
     * CXF extensions, endpoint wrappers, etc.
     *
     * @param sei the fully qualified name of the service class for which the client should be created
     * @param bus the bus to use with {@link GeneratedClassClassLoaderCapture} set properly.
     */
    public static void createClient(String sei, Bus bus) {

        JaxWsClientFactoryBean factoryBean = new JaxWsClientFactoryBean();
        factoryBean.setBus(bus);
        try {
            factoryBean.setServiceClass(Thread.currentThread().getContextClassLoader().loadClass(sei));
            factoryBean.create();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load " + sei, e);
        }
    }

    public static String getNameSpaceFromClassInfo(ClassInfo wsClassInfo) {
        String pkg = wsClassInfo.name().toString();
        int idx = pkg.lastIndexOf('.');
        if (idx != -1 && idx < pkg.length() - 1) {
            pkg = pkg.substring(0, idx);
        }
        // TODO XRootElement then XmlSchema then derived of package
        String[] strs = pkg.split("\\.");
        StringBuilder b = new StringBuilder("http://");
        for (int i = strs.length - 1; i >= 0; i--) {
            if (i != strs.length - 1) {
                b.append(".");
            }
            b.append(strs[i]);
        }
        b.append("/");
        return b.toString();
    }

    public static void walkParents(
            Class<?> cl,
            Consumer<String> consumer) {
        consumer.accept(cl.getName());
        final Class<?> parent = cl.getSuperclass();
        if (Object.class != parent && parent != null) {
            walkParents(parent, consumer);
        }
        for (Class<?> intf : cl.getInterfaces()) {
            walkParents(intf, consumer);
        }
    }

}
