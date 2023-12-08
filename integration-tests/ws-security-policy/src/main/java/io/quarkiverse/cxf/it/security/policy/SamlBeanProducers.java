package io.quarkiverse.cxf.it.security.policy;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Collections;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.bean.AttributeBean;
import org.apache.wss4j.common.saml.bean.AttributeStatementBean;
import org.apache.wss4j.common.saml.bean.ConditionsBean;
import org.apache.wss4j.common.saml.bean.KeyInfoBean;
import org.apache.wss4j.common.saml.bean.KeyInfoBean.CERT_IDENTIFIER;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.common.saml.bean.Version;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.dom.WSConstants;

public class SamlBeanProducers {

    @Produces
    @ApplicationScoped
    @Named
    public CallbackHandler saml2CallbackHandler() {
        return new SamlCallbackHandler(Version.SAML_20);
    }

    @Produces
    @ApplicationScoped
    @Named
    public CallbackHandler saml1CallbackHandler() {
        return new SamlCallbackHandler(Version.SAML_11);
    }

    /**
     * A CallbackHandler instance that is used by the STS to mock up a SAML Attribute Assertion.
     * <p>
     * Adapted from <a href=
     * "https://github.com/apache/cxf/blob/cxf-4.0.3/systests/ws-security/src/test/java/org/apache/cxf/systest/ws/saml/client/SamlCallbackHandler.java">https://github.com/apache/cxf/blob/cxf-4.0.3/systests/ws-security/src/test/java/org/apache/cxf/systest/ws/saml/client/SamlCallbackHandler.java</a>
     */
    public static class SamlCallbackHandler implements CallbackHandler {
        private final Version samlVersion;
        private String confirmationMethod;
        private CERT_IDENTIFIER keyInfoIdentifier = CERT_IDENTIFIER.X509_CERT;
        private final boolean signAssertion = false;
        private ConditionsBean conditions;
        private String cryptoAlias = "alice";
        private String cryptoPassword = "password";
        private String cryptoKeystoreFile = "alice.p12";
        private String signatureAlgorithm = WSConstants.RSA_SHA1;
        private String digestAlgorithm = WSConstants.SHA1;

        public SamlCallbackHandler(Version samlVersion) {
            this.samlVersion = samlVersion;
            switch (samlVersion) {
                case SAML_20:
                    this.confirmationMethod = SAML2Constants.CONF_SENDER_VOUCHES;
                    break;
                case SAML_11:
                case SAML_10:
                    this.confirmationMethod = SAML1Constants.CONF_SENDER_VOUCHES;
                    break;
                default:
                    throw new IllegalStateException("Unexpected " + Version.class.getName() + ": " + samlVersion);
            }
        }

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof SAMLCallback) {
                    SAMLCallback callback = (SAMLCallback) callbacks[i];
                    callback.setSamlVersion(samlVersion);
                    if (conditions != null) {
                        callback.setConditions(conditions);
                    }

                    callback.setIssuer("sts");
                    String subjectName = "uid=sts-client,o=mock-sts.com";
                    String subjectQualifier = "www.mock-sts.com";
                    SubjectBean subjectBean = new SubjectBean(
                            subjectName, subjectQualifier, confirmationMethod);
                    if (SAML2Constants.CONF_HOLDER_KEY.equals(confirmationMethod)
                            || SAML1Constants.CONF_HOLDER_KEY.equals(confirmationMethod)) {
                        try {
                            KeyInfoBean keyInfo = createKeyInfo();
                            subjectBean.setKeyInfo(keyInfo);
                        } catch (Exception ex) {
                            throw new IOException("Problem creating KeyInfo: " + ex.getMessage());
                        }
                    }
                    callback.setSubject(subjectBean);

                    AttributeStatementBean attrBean = new AttributeStatementBean();
                    attrBean.setSubject(subjectBean);

                    AttributeBean attributeBean = new AttributeBean();
                    switch (samlVersion) {
                        case SAML_20:
                            attributeBean.setQualifiedName("subject-role");
                            break;
                        case SAML_11:
                        case SAML_10:
                            attributeBean.setSimpleName("subject-role");
                            attributeBean.setQualifiedName("http://custom-ns");
                            break;
                        default:
                            throw new IllegalStateException("Unexpected " + Version.class.getName() + ": " + samlVersion);
                    }
                    attributeBean.addAttributeValue("system-user");
                    attrBean.setSamlAttributes(Collections.singletonList(attributeBean));
                    callback.setAttributeStatementData(Collections.singletonList(attrBean));
                    callback.setSignatureAlgorithm(signatureAlgorithm);
                    callback.setSignatureDigestAlgorithm(digestAlgorithm);

                    Crypto crypto = io.quarkiverse.cxf.it.security.policy.CryptoProducers.createCrypto("pkcs12", cryptoAlias,
                            cryptoPassword, cryptoKeystoreFile);
                    callback.setIssuerCrypto(crypto);
                    callback.setIssuerKeyName(cryptoAlias);
                    callback.setIssuerKeyPassword(cryptoPassword);
                    callback.setSignAssertion(signAssertion);
                }
            }
        }

        protected KeyInfoBean createKeyInfo() throws Exception {
            Crypto crypto = io.quarkiverse.cxf.it.security.policy.CryptoProducers.createCrypto("jks", cryptoAlias,
                    cryptoPassword, cryptoKeystoreFile);
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias(cryptoAlias);
            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);

            KeyInfoBean keyInfo = new KeyInfoBean();
            keyInfo.setCertIdentifer(keyInfoIdentifier);
            if (keyInfoIdentifier == CERT_IDENTIFIER.X509_CERT) {
                keyInfo.setCertificate(certs[0]);
            } else if (keyInfoIdentifier == CERT_IDENTIFIER.KEY_VALUE) {
                keyInfo.setPublicKey(certs[0].getPublicKey());
            }

            return keyInfo;
        }

    }

}
