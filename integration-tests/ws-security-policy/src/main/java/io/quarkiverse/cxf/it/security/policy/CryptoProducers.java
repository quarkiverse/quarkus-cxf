package io.quarkiverse.cxf.it.security.policy;

import java.util.Properties;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.wss4j.common.ext.WSSecurityException;

public class CryptoProducers {

    private final PasswordEncryptor dummyPasswordEncryptor = new PasswordEncryptor() {

        @Override
        public String encrypt(String password) {
            return password;
        }

        @Override
        public String decrypt(String encryptedPassword) {
            return encryptedPassword;
        }
    };

    @Produces
    @ApplicationScoped
    @Named
    public Crypto bobCrypto() {
        Properties props = new Properties();
        props.put("org.apache.ws.security.crypto.provider", "org.apache.ws.security.components.crypto.Merlin");
        props.put("org.apache.ws.security.crypto.merlin.keystore.type", "jks");
        props.put("org.apache.ws.security.crypto.merlin.keystore.password", "password");
        props.put("org.apache.ws.security.crypto.merlin.keystore.alias", "bob");
        props.put("org.apache.ws.security.crypto.merlin.file", "bob.jks");
        try {
            return CryptoFactory.getInstance(props, CryptoFactory.class.getClassLoader(), dummyPasswordEncryptor);
        } catch (WSSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Produces
    @ApplicationScoped
    @Named
    public Crypto aliceCrypto() {
        Properties props = new Properties();
        props.put("org.apache.ws.security.crypto.provider", "org.apache.ws.security.components.crypto.Merlin");
        props.put("org.apache.ws.security.crypto.merlin.keystore.type", "jks");
        props.put("org.apache.ws.security.crypto.merlin.keystore.password", "password");
        props.put("org.apache.ws.security.crypto.merlin.keystore.alias", "alice");
        props.put("org.apache.ws.security.crypto.merlin.file", "alice.jks");
        try {
            return CryptoFactory.getInstance(props, CryptoFactory.class.getClassLoader(), dummyPasswordEncryptor);
        } catch (WSSecurityException e) {
            throw new RuntimeException(e);
        }
    }

}
