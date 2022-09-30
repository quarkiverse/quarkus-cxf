package io.quarkiverse.cxf.deployment;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.cxf.Bus;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;

public class CxfDeploymentUtils {

    private CxfDeploymentUtils() {
    }

    public static Stream<AnnotationInstance> webServiceAnnotations(IndexView index) {
        return Stream.of(CxfDotNames.WEBSERVICE_ANNOTATION, CxfDotNames.WEBSERVICE_PROVIDER_ANNOTATION)
                .map(index::getAnnotations)
                .flatMap(Collection::stream)
                .filter(annotation -> annotation.target().kind() == AnnotationTarget.Kind.CLASS);
    }

    public static QuarkusJaxWsServiceFactoryBean createQuarkusJaxWsServiceFactoryBean(String sei, Bus bus) {
        QuarkusJaxWsServiceFactoryBean jaxwsFac = new QuarkusJaxWsServiceFactoryBean();
        jaxwsFac.setBus(bus);
        // TODO here add all class
        try {
            jaxwsFac.setServiceClass(Thread.currentThread().getContextClassLoader().loadClass(sei));
            jaxwsFac.create();
            return jaxwsFac;
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
