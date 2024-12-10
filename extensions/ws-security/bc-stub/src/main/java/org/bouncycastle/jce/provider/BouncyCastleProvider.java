package org.bouncycastle.jce.provider;

import java.security.Provider;

public class BouncyCastleProvider extends Provider {
    public BouncyCastleProvider(String name, String versionStr, String info) {
        super(name, versionStr, info);
        throw new UnsupportedOperationException("Exclude io.quarkiverse.cxf:quarkus-cxf-bc-stub from"
                + " io.quarkiverse.cxf:quarkus-cxf-rt-ws-security dependencies and add Bouncy Castle instead");
    }
}
