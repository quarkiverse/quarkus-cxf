package io.quarkiverse.cxf.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.mail.Provider;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;

/**
 * {@link BuildStep}s related to {@code jakarta.activation}
 */
class JakartaActivationProcessor {

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "jakarta.activation:jakarta.activation-api",
                "org.eclipse.angus:angus-activation",
                "org.eclipse.angus:angus-mail")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    void process(CombinedIndexBuildItem combinedIndex, BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ServiceProviderBuildItem> services) {
        List<String> providers = resources("META-INF/services/jakarta.mail.Provider")
                .flatMap(this::lines)
                .filter(s -> !s.startsWith("#"))
                .collect(Collectors.toList());

        List<String> streamProviders = resources("META-INF/services/jakarta.mail.util.StreamProvider")
                .flatMap(this::lines)
                .filter(s -> !s.startsWith("#"))
                .collect(Collectors.toList());

        List<String> imp1 = providers.stream()
                .map(this::loadClass)
                .map(this::instantiate)
                .map(Provider.class::cast)
                .map(Provider::getClassName)
                .collect(Collectors.toList());

        List<String> imp2 = Stream.of("META-INF/javamail.default.providers", "META-INF/javamail.providers")
                .flatMap(this::resources)
                .flatMap(this::lines)
                .filter(s -> !s.startsWith("#"))
                .flatMap(s -> Stream.of(s.split(";")))
                .map(String::trim)
                .filter(s -> s.startsWith("class="))
                .map(s -> s.substring("class=".length()))
                .collect(Collectors.toList());

        List<String> imp3 = resources("META-INF/mailcap")
                .flatMap(this::lines)
                .filter(s -> !s.startsWith("#"))
                .flatMap(s -> Stream.of(s.split(";")))
                .map(String::trim)
                .filter(s -> s.startsWith("x-java-content-handler="))
                .map(s -> s.substring("x-java-content-handler=".length()))
                .collect(Collectors.toList());

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false,
                Stream.concat(providers.stream(),
                        Stream.concat(streamProviders.stream(),
                                Stream.concat(imp1.stream(), Stream.concat(imp2.stream(), imp3.stream()))))
                        .distinct()
                        .toArray(String[]::new)));

        //jakarta activation spi
        combinedIndex.getIndex().getKnownClasses()
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .filter(name -> name.startsWith("jakarta.activation.spi."))
                .forEach(name -> combinedIndex.getIndex().getKnownDirectImplementors(DotName.createSimple(name))
                        .stream()
                        .forEach(service -> services.produce(
                                new ServiceProviderBuildItem(name, service.name().toString()))));
    }

    private Stream<URL> resources(String path) {
        try {
            return enumerationAsStream(getClass().getClassLoader().getResources(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Class<?> loadClass(String name) {
        try {
            return getClass().getClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T instantiate(Class<T> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<String> lines(URL url) {
        try (InputStream is = url.openStream()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                return br.lines().collect(Collectors.toList()).stream();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new Iterator<T>() {
                            @Override
                            public T next() {
                                return e.nextElement();
                            }

                            @Override
                            public boolean hasNext() {
                                return e.hasMoreElements();
                            }
                        },
                        Spliterator.ORDERED),
                false);
    }

    @BuildStep
    public void nativeResources(BuildProducer<NativeImageResourceBuildItem> nativeResources) {
        nativeResources.produce(new NativeImageResourceBuildItem(
                "META-INF/services/jakarta.mail.Provider",
                "META-INF/services/jakarta.mail.util.StreamProvider",
                "META-INF/javamail.charset.map",
                "META-INF/javamail.default.address.map",
                "META-INF/javamail.default.providers",
                "META-INF/javamail.address.map",
                "META-INF/javamail.providers",
                "META-INF/mailcap",
                "META-INF/mailcap.default",
                "META-INF/mimetypes.default"));
    }

    @BuildStep
    void reflectiveClasses(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        final IndexView index = combinedIndexBuildItem.getIndex();

        index.getAllKnownImplementors(DotName.createSimple("javax.activation.DataContentHandler")).stream()
                .map(classInfo -> classInfo.name().toString())
                .map(className -> new ReflectiveClassBuildItem(false, false, className))
                .forEach(reflectiveClasses::produce);

        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, "java.beans.Beans"));
    }
}
