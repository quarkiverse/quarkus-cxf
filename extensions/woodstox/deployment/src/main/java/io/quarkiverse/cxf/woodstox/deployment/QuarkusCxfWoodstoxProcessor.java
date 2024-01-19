package io.quarkiverse.cxf.woodstox.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import org.codehaus.stax2.validation.XMLValidationSchemaFactory;

import com.ctc.wstx.shaded.msv.relaxng_datatype.DatatypeLibraryFactory;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;

public class QuarkusCxfWoodstoxProcessor {

    @BuildStep
    void registerServices(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        Stream.concat(
                Stream.of(
                        XMLEventFactory.class,
                        XMLInputFactory.class,
                        XMLOutputFactory.class,
                        DatatypeLibraryFactory.class)
                        .map(Class::getName),
                Stream.of(
                        XMLValidationSchemaFactory.INTERNAL_ID_SCHEMA_DTD,
                        XMLValidationSchemaFactory.INTERNAL_ID_SCHEMA_RELAXNG,
                        XMLValidationSchemaFactory.INTERNAL_ID_SCHEMA_W3C,
                        XMLValidationSchemaFactory.INTERNAL_ID_SCHEMA_TREX)
                        .map(schemaId -> XMLValidationSchemaFactory.class.getName() + "." + schemaId))
                .forEach(serviceName -> {
                    try {
                        final Set<String> names = ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(),
                                ServiceProviderBuildItem.SPI_ROOT + serviceName);
                        serviceProvider.produce(new ServiceProviderBuildItem(serviceName, new ArrayList<>(names)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

}
