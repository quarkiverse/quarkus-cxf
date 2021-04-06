package io.quarkiverse.cxf;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.arc.Unremovable;

/**
 * Base producer class for setting up CXF client proxies.
 * <p>
 * During augementation (build-time) a bean is created derived from this class for each SEI. The producing method calls
 * loadCxfClient() to get a WS client proxy.
 * <p>
 * Notice the InjectionPoint parameter present in signature of loadCxfClient. Via that meta information we calculate the
 * proper configuration to use.
 */
@ApplicationScoped
@Unremovable
public class CxfInfoProducer {
    private static final Logger LOGGER = Logger.getLogger(CxfInfoProducer.class);
    private static final String DEFAULT_EP_ADDR = "http://localhost:8080";

    @Inject
    CxfConfig config;

    @Produces
    @CXFClient
    public CXFClientInfo produceClientInfo() {
        return selectorCXFClientInfo(config, null, null);
    }

    /**
     * Calculates the client info to use for producing a JAXWS client proxy.
     *
     * @param cxfConfig The current configuration
     * @param ip Meta information about where injection of client proxy takes place
     * @param meta The default to return
     * @return not null
     */
    static private CXFClientInfo selectorCXFClientInfo(
            CxfConfig cxfConfig,
            InjectionPoint ip,
            CXFClientInfo meta) {
        CXFClientInfo info = new CXFClientInfo(meta);

        // If injection point is annotated with @CXFClient then determine a
        // configuration by looking up annotated config value:

        if (ip.getAnnotated().isAnnotationPresent(CXFClient.class)) {
            CXFClient anno = ip.getAnnotated().getAnnotation(CXFClient.class);
            String configKey = anno.value();

            if (cxfConfig.isClientPresent(configKey)) {
                return info.withConfig(cxfConfig.getClient(configKey));
            }

            // If config-key is present and not default: This is an error:
            if (configKey != null && !configKey.isEmpty()) {
                throw new IllegalStateException(format(
                        "client config key %s does not exist. This is illegal.",
                        configKey));
            }
        }
        // User did not specify any client config value. Thus we make a smart guess
        // about which configuration is to be used.
        //
        // Determine all matching configurations for given SEI
        List<String> keylist = cxfConfig.clients
                .entrySet()
                .stream()
                .filter(kv -> kv.getValue() != null)
                .filter(kv -> kv.getValue().serviceInterface.isPresent())
                .filter(kv -> kv.getValue().serviceInterface.get().equals(meta.getSei()))
                .filter(kv -> kv.getValue().alternative == false)
                .map(Map.Entry::getKey)
                .collect(toList());

        // keylist contains all configurations for given SEI. It is illegal to have multiple matching
        // configurations.

        if (keylist.size() > 1) {
            String fmt;
            fmt = "multiple client configurations found applicable for SEI(%s): %s. This is illegal. Consider to " +
                    "remove all but one applicable configurations by applying config property '*.alternative = false'.";
            throw new IllegalStateException(format(fmt, meta.getSei(), join("|", keylist)));
        }

        // It is legal to have no matching configuration. Then we go ahead and use default values derived from
        // the service itself.

        if (keylist.isEmpty()) {
            String fmt;
            fmt = "no matching configuration found for SEI %s, using derived value %s.";
            LOGGER.warn(format(fmt, meta.getSei(), meta));
            return meta;
        }

        return info.withConfig(cxfConfig.clients.get(keylist.get(0)));
    }

}
