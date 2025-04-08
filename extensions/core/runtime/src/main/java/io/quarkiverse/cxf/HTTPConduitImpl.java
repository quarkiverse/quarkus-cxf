package io.quarkiverse.cxf;

import java.io.IOException;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.URLConnectionHTTPConduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import io.quarkiverse.cxf.vertx.http.client.HttpClientPool;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit;
import io.quarkus.runtime.annotations.ConfigDocEnumValue;

public enum HTTPConduitImpl implements HTTPConduitSpec {

    @ConfigDocEnumValue("QuarkusCXFDefault")
    QuarkusCXFDefault {

        private HTTPConduitImpl defaultHTTPConduitImpl;

        @Override
        public HTTPConduitImpl resolveDefault() {
            HTTPConduitImpl result;
            if ((result = defaultHTTPConduitImpl) == null) {
                defaultHTTPConduitImpl = result = findDefaultHTTPConduitImpl();
            }
            return result;
        }
    },
    @ConfigDocEnumValue("CXFDefault")
    CXFDefault {
        @Override
        public HTTPConduitImpl resolveDefault() {
            return URLConnectionHTTPConduitFactory;
        }
    },
    @ConfigDocEnumValue("VertxHttpClientHTTPConduitFactory")
    VertxHttpClientHTTPConduitFactory {
        @Override
        public HTTPConduit createConduit(CXFClientInfo cxfClientInfo, HttpClientPool httpClientPool, Bus b,
                EndpointInfo localInfo,
                EndpointReferenceType target) throws IOException {
            return new VertxHttpClientHTTPConduit(cxfClientInfo, b, localInfo, target, httpClientPool);
        }

        @Override
        public TLSClientParameters createTLSClientParameters(CXFClientInfo cxfClientInfo) {
            if (cxfClientInfo.getHostnameVerifier() != null) {
                throw new IllegalStateException(
                        getConduitDescription() + " does not support quarkus.cxf.client."
                                + cxfClientInfo.getConfigKey() + ".hostname-verifier."
                                + " AllowAllHostnameVerifier can be replaced by using a named TLS configuration"
                                + " (via quarkus.cxf.client."
                                + cxfClientInfo.getConfigKey() + ".tls-configuration-name)"
                                + " with quarkus.tls.\"tls-bucket-name\".hostname-verification-algorithm set to NONE");
            }
            return new QuarkusTLSClientParameters(cxfClientInfo.getTlsConfigurationName(), cxfClientInfo.getTlsConfiguration());
        }

    },
    @ConfigDocEnumValue("URLConnectionHTTPConduitFactory")
    URLConnectionHTTPConduitFactory {
        @Override
        public HTTPConduit createConduit(CXFClientInfo cxfClientInfo, HttpClientPool httpClientPool, Bus b,
                EndpointInfo localInfo,
                EndpointReferenceType target) throws IOException {
            return new URLConnectionHTTPConduit(b, localInfo, target);
        }
    };

    public static HTTPConduitImpl findDefaultHTTPConduitImpl() {
        if (QuarkusHTTPConduitFactory.defaultHTTPConduitImpl == null) {
            final String defaultName = System.getenv(QuarkusHTTPConduitFactory.QUARKUS_CXF_DEFAULT_HTTP_CONDUIT_FACTORY);
            QuarkusHTTPConduitFactory.defaultHTTPConduitImpl = defaultName == null || defaultName.isEmpty()
                    ? VertxHttpClientHTTPConduitFactory
                    : valueOf(defaultName);
        }
        return QuarkusHTTPConduitFactory.defaultHTTPConduitImpl;
    }

    @Override
    public HTTPConduit createConduit(CXFClientInfo cxfClientInfo, HttpClientPool httpClientPool, Bus b, EndpointInfo localInfo,
            EndpointReferenceType target)
            throws IOException {
        throw new IllegalStateException(
                "Call " + HTTPConduitImpl.class.getName() + ".resolveDefault() before calling createConduit()");
    }

    @Override
    public String getConduitDescription() {
        return "http-conduit-factory = " + name();
    }

}
