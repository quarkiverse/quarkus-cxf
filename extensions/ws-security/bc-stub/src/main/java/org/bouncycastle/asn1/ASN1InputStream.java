package org.bouncycastle.asn1;

import java.io.IOException;

public class ASN1InputStream extends java.io.FilterInputStream {
    public ASN1InputStream(byte[] input) {
        super(null);
        throw new UnsupportedOperationException("Exclude io.quarkiverse.cxf:quarkus-cxf-bc-stub from"
                + " io.quarkiverse.cxf:quarkus-cxf-rt-ws-security dependencies and add Bouncy Castle instead");
    }

    public ASN1Primitive readObject() throws IOException {
        throw new UnsupportedOperationException("Exclude io.quarkiverse.cxf:quarkus-cxf-bc-stub from"
                + " io.quarkiverse.cxf:quarkus-cxf-rt-ws-security dependencies and add Bouncy Castle instead");
    }

}
