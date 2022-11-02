package io.quarkiverse.cxf.transport.http.hc5.graal;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.apache.hc.core5.http2.ssl.H2TlsSupport;
import org.apache.hc.core5.reactor.ssl.TlsDetails;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.apache.hc.client5.http.ssl.ConscryptClientTlsStrategy")
final class Target_org_apache_hc_client5_http_ssl_ConscryptClientTlsStrategy {

    @Substitute
    public static boolean isSupported() {
        return false;
    }

    @Substitute
    TlsDetails createTlsDetails(final SSLEngine sslEngine) {
        return null;
    }

    @Substitute
    void applyParameters(final SSLEngine sslEngine, final SSLParameters sslParameters, final String[] appProtocols) {
        H2TlsSupport.setApplicationProtocols(sslParameters, appProtocols);
        sslEngine.setSSLParameters(sslParameters);
    }

}

public class CxfHttpAsyncSubstitutions {

}
