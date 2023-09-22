package io.quarkiverse.cxf.it.security.policy;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class NoopHostnameVerifier implements HostnameVerifier {

    @Override
    public boolean verify(final String s, final SSLSession sslSession) {
        return true;
    }

    @Override
    public final String toString() {
        return "NoopHostnameVerifier";
    }

}