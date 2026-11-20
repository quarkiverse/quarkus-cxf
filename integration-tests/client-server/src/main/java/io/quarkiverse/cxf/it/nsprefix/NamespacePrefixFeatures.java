package io.quarkiverse.cxf.it.nsprefix;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import org.apache.cxf.Bus;
import org.apache.cxf.databinding.AbstractDataBinding;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.Feature;

import io.quarkiverse.cxf.it.HelloService;

/**
 * {@link Feature}s setting the preferred namespace prefixes on the data binding of the given service endpoint.
 */
public class NamespacePrefixFeatures {

    static final Map<String, String> PREFIXES_BY_NAMESPACE = Map.of(HelloService.NS, "hello");

    @ApplicationScoped
    @Named("namespaceMapFeature")
    public static class NamespaceMapFeature implements AbstractPortableFeature {
        @Override
        public void initialize(Server server, Bus bus) {
            dataBinding(server).setNamespaceMap(PREFIXES_BY_NAMESPACE);
        }
    }

    @ApplicationScoped
    @Named("contextualNamespaceMapFeature")
    public static class ContextualNamespaceMapFeature implements AbstractPortableFeature {
        @Override
        public void initialize(Server server, Bus bus) {
            dataBinding(server).setContextualNamespaceMap(PREFIXES_BY_NAMESPACE);
        }
    }

    static AbstractDataBinding dataBinding(Server server) {
        return (AbstractDataBinding) server.getEndpoint().getService().getDataBinding();
    }
}
