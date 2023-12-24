package io.quarkiverse.cxf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;

/**
 * A {@link HTTPConduitConfigurer} able to configure conduits by address.
 */
@ApplicationScoped
public class QuarkusHttpConduitConfigurer implements HTTPConduitConfigurer {
    private final Map<String, List<Consumer<HTTPConduit>>> configurersByAddress = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        BusFactory.getDefaultBus().setExtension(this, HTTPConduitConfigurer.class);
    }

    @Override
    public void configure(String name, String address, HTTPConduit conduit) {
        final List<Consumer<HTTPConduit>> configurers = configurersByAddress.get(address);
        if (configurers != null) {
            configurers.forEach(configurer -> configurer.accept(conduit));
        }
    }

    /**
     * Add a {@code configurer} that will be applied only to the conduit associated with the given {@code address}.
     *
     * @param address the endpoint address for which the give {@code configurer} should be registered
     * @param configurer the {@code configurer} to apply to the conduit associated with the given {@code address}.
     */
    public void addConfigurer(String address, Consumer<HTTPConduit> configurer) {
        configurersByAddress.compute(address, (k, v) -> {
            if (v == null) {
                v = new ArrayList<>();
            }
            v.add(configurer);
            return v;
        });
    }

}
