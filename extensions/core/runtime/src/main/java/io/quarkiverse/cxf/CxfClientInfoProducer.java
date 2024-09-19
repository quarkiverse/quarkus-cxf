package io.quarkiverse.cxf;

import java.util.Optional;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;

import io.quarkiverse.cxf.CxfFixedConfig.ClientFixedConfig;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.arc.Arc;

/**
 * Produces {@link CXFClientInfo} beans.
 */
public class CxfClientInfoProducer extends CxfClientProducer {

    /**
     * Must be public, otherwise: java.lang.VerifyError: Bad access to protected data in invokevirtual
     */
    @Produces
    @CXFClient
    public CXFClientInfo loadCxfClientInfo(InjectionPoint ip) {
        final CXFClient cxfClient = ip.getAnnotated().getAnnotation(CXFClient.class);
        if (cxfClient == null) {
            throw new IllegalStateException(CXFClientInfo.class.getName() + " can only be injected with @"
                    + CXFClient.class.getName() + " annotation with value specified, e.g. @" + CXFClient.class.getSimpleName()
                    + "(\"myClient\"), where \"myClient\" is the key used for configuring the given client in application.properties");
        }
        final String key = cxfClient.value();
        if (key == null) {
            throw new IllegalStateException(CXFClientInfo.class.getName() + " can only be injected with @"
                    + CXFClient.class.getName() + " annotation with value specified, e.g. @" + CXFClient.class.getSimpleName()
                    + "(\"myClient\"), where \"myClient\" is the key used for configuring the given client in application.properties");
        }
        final ClientFixedConfig fixedClientConfig = fixedConfig.clients().get(key);
        if (fixedClientConfig == null) {
            throw new IllegalStateException(
                    "Could not find quarkus.cxf.client." + key + ".service-interface in application configuration for @"
                            + CXFClient.class.getSimpleName() + "(\"" + key + "\")");
        }
        final Optional<String> serviceInterface = fixedClientConfig.serviceInterface();
        if (serviceInterface.isEmpty()) {
            throw new IllegalStateException(
                    "Could not find quarkus.cxf.client." + key + ".service-interface in application configuration for @"
                            + CXFClient.class.getSimpleName() + "(\"" + key + "\")");
        }
        final CXFClientData meta = Arc.container().<CXFClientData> instance(serviceInterface.get()).get();
        return selectorCXFClientInfo(config, fixedConfig, ip, meta, vertx);
    }

}
