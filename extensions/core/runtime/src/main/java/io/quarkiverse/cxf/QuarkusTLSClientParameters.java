package io.quarkiverse.cxf;

import java.util.List;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.FiltersType;

import io.quarkus.tls.TlsConfiguration;

public class QuarkusTLSClientParameters extends TLSClientParameters {

    private final String tlsConfigurationName;
    private final TlsConfiguration tlsConfiguration;

    public QuarkusTLSClientParameters(String tlsConfigurationName, TlsConfiguration tlsConfiguration) {
        super();
        this.tlsConfigurationName = tlsConfigurationName;
        this.tlsConfiguration = tlsConfiguration;
    }

    public String getTlsConfigurationName() {
        return tlsConfigurationName;
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
