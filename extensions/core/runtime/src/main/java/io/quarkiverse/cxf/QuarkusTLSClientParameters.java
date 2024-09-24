package io.quarkiverse.cxf;

import java.util.List;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.FiltersType;

import io.quarkus.tls.TlsConfiguration;

public class QuarkusTLSClientParameters extends TLSClientParameters {

    private final TlsConfiguration tlsConfiguration;

    public QuarkusTLSClientParameters(TlsConfiguration tlsConfiguration) {
        super();
        this.tlsConfiguration = tlsConfiguration;
    }

    public TlsConfiguration getTlsConfiguration() {
        return tlsConfiguration;
    }

    @Override
    public List<String> getCipherSuites() {
        return super.getCipherSuites();
    }

    @Override
    public FiltersType getCipherSuitesFilter() {
        return super.getCipherSuitesFilter();
    }

}
