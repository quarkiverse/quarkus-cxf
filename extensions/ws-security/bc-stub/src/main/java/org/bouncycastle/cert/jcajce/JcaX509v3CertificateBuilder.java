package org.bouncycastle.cert.jcajce;

import java.math.BigInteger;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

public class JcaX509v3CertificateBuilder {
    public JcaX509v3CertificateBuilder(X500Name issuer, BigInteger serial, Date notBefore, Date notAfter, X500Name subject,
            SubjectPublicKeyInfo publicKey) {
        throw new UnsupportedOperationException("Exclude io.quarkiverse.cxf:quarkus-cxf-bc-stub from"
                + " io.quarkiverse.cxf:quarkus-cxf-rt-ws-security dependencies and add Bouncy Castle instead");
    }

}
