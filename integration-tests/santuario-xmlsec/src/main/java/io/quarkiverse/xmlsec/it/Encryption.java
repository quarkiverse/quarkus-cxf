/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.quarkiverse.xmlsec.it;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.security.Key;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.xml.security.Init;
import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.stax.ext.InboundXMLSec;
import org.apache.xml.security.stax.ext.OutboundXMLSec;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.XMLSec;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.ext.XMLSecurityProperties;
import org.apache.xml.security.stax.securityToken.SecurityTokenConstants;
import org.apache.xml.security.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Some utility methods for encrypting/decrypting documents
 * <p>
 * Adapted form <a href=
 * "https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-encryption/src/test/java/org/apache/coheigea/santuario/xmlencryption/EncryptionUtils.java">https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-encryption/src/test/java/org/apache/coheigea/santuario/xmlencryption/EncryptionUtils.java</a>
 * by <a href="https://github.com/coheigea">Colm O hEigeartaigh</a>
 */
public enum Encryption {
    dom() {

        /**
         * Encrypt the document using the DOM API of Apache Santuario - XML Security for Java.
         * It encrypts a list of QNames that it finds in the Document via XPath. If a wrappingKey
         * is supplied, this is used to encrypt the encryptingKey + place it in an EncryptedKey
         * structure.
         */
        @Override
        public byte[] encrypt(byte[] plaintext, List<QName> namesToEncrypt, String algorithm, Key encryptingKey,
                String keyTransportAlgorithm, PublicKey wrappingKey, boolean content) {
            try (ByteArrayInputStream in = new ByteArrayInputStream(plaintext)) {
                Document document = createDocumentBuilder(false, false).parse(in);

                XMLCipher cipher = XMLCipher.getInstance(algorithm);
                cipher.init(XMLCipher.ENCRYPT_MODE, encryptingKey);

                if (wrappingKey != null) {
                    XMLCipher newCipher = XMLCipher.getInstance(keyTransportAlgorithm);
                    newCipher.init(XMLCipher.WRAP_MODE, wrappingKey);

                    EncryptedKey encryptedKey = newCipher.encryptKey(document, encryptingKey);
                    // Create a KeyInfo for the EncryptedKey
                    KeyInfo encryptedKeyKeyInfo = encryptedKey.getKeyInfo();
                    if (encryptedKeyKeyInfo == null) {
                        encryptedKeyKeyInfo = new KeyInfo(document);
                        encryptedKeyKeyInfo.getElement().setAttributeNS(
                                "http://www.w3.org/2000/xmlns/", "xmlns:dsig", "http://www.w3.org/2000/09/xmldsig#");
                        encryptedKey.setKeyInfo(encryptedKeyKeyInfo);
                    }
                    encryptedKeyKeyInfo.add(wrappingKey);

                    // Create a KeyInfo for the EncryptedData
                    EncryptedData builder = cipher.getEncryptedData();
                    KeyInfo builderKeyInfo = builder.getKeyInfo();
                    if (builderKeyInfo == null) {
                        builderKeyInfo = new KeyInfo(document);
                        builderKeyInfo.getElement().setAttributeNS(
                                "http://www.w3.org/2000/xmlns/", "xmlns:dsig", "http://www.w3.org/2000/09/xmldsig#");
                        builder.setKeyInfo(builderKeyInfo);
                    }

                    builderKeyInfo.add(encryptedKey);
                }

                for (QName nameToEncrypt : namesToEncrypt) {
                    NodeList elementsToEncrypt = document.getElementsByTagName(nameToEncrypt.getLocalPart());
                    for (int i = 0; i < elementsToEncrypt.getLength(); i++) {
                        Element elementToEncrypt = (Element) elementsToEncrypt.item(i);
                        document = cipher.doFinal(document, elementToEncrypt, content);
                    }
                }

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    XMLUtils.outputDOM(document, baos);
                    return baos.toByteArray();
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

        /**
         * Decrypt the document using the DOM API of Apache Santuario - XML Security for Java.
         */
        @Override
        public byte[] decrypt(byte[] encrypted, String algorithm, Key privateKey) {
            try (ByteArrayInputStream in = new ByteArrayInputStream(encrypted)) {
                DocumentBuilder builder = Encryption.createDocumentBuilder(false, false);
                Document document = builder.parse(in);

                // Decrypt using DOM
                XMLCipher cipher = XMLCipher.getInstance(algorithm);
                cipher.init(XMLCipher.DECRYPT_MODE, null);
                cipher.setKEK(privateKey);

                NodeList nodeList = document.getElementsByTagNameNS(
                        XMLSecurityConstants.TAG_xenc_EncryptedData.getNamespaceURI(),
                        XMLSecurityConstants.TAG_xenc_EncryptedData.getLocalPart());

                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element ee = (Element) nodeList.item(i);
                    cipher.doFinal(document, ee);
                }

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    XMLUtils.outputDOM(document, baos);
                    return baos.toByteArray();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    },
    stax() {
        /**
         * Encrypt the document using the StAX API of Apache Santuario - XML Security for Java. If
         * a wrappingKey is supplied, this is used to encrypt the encryptingKey + place it in an
         * EncryptedKey structure.
         */
        @Override
        public byte[] encrypt(byte[] plaintext, List<QName> namesToEncrypt, String algorithm, Key encryptingKey,
                String keyTransportAlgorithm, PublicKey wrappingKey, boolean content) {
            // Set up the Configuration
            XMLSecurityProperties properties = new XMLSecurityProperties();
            List<XMLSecurityConstants.Action> actions = new ArrayList<XMLSecurityConstants.Action>();
            actions.add(XMLSecurityConstants.ENCRYPTION);
            properties.setActions(actions);

            properties.setEncryptionSymAlgorithm(algorithm);
            properties.setEncryptionKey(encryptingKey);
            properties.setEncryptionKeyTransportAlgorithm(keyTransportAlgorithm);
            properties.setEncryptionTransportKey(wrappingKey);
            properties.setEncryptionKeyIdentifier(
                    SecurityTokenConstants.KeyIdentifier_X509KeyIdentifier);

            SecurePart.Modifier modifier = SecurePart.Modifier.Content;
            if (!content) {
                modifier = SecurePart.Modifier.Element;
            }
            for (QName nameToEncrypt : namesToEncrypt) {
                SecurePart securePart = new SecurePart(nameToEncrypt, modifier);
                properties.addEncryptionPart(securePart);
            }

            try {
                OutboundXMLSec outboundXMLSec = XMLSec.getOutboundXMLSec(properties);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                XMLStreamWriter xmlStreamWriter = outboundXMLSec.processOutMessage(baos, "UTF-8");

                XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
                XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(plaintext));

                XmlReaderToWriter.writeAll(xmlStreamReader, xmlStreamWriter);
                xmlStreamWriter.close();

                return baos.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Decrypt the document using the StAX API of Apache Santuario - XML Security for Java.
         */
        @Override
        public byte[] decrypt(byte[] encrypted, String algorithm, Key privateKey) {
            // Set up the Configuration
            XMLSecurityProperties properties = new XMLSecurityProperties();
            List<XMLSecurityConstants.Action> actions = new ArrayList<XMLSecurityConstants.Action>();
            actions.add(XMLSecurityConstants.ENCRYPTION);
            properties.setActions(actions);

            properties.setDecryptionKey(privateKey);

            ByteArrayOutputStream baos;
            try {
                InboundXMLSec inboundXMLSec = XMLSec.getInboundWSSec(properties);

                XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
                final XMLStreamReader xmlStreamReader = xmlInputFactory
                        .createXMLStreamReader(new ByteArrayInputStream(encrypted));
                XMLStreamReader securityStreamReader = inboundXMLSec.processInMessage(xmlStreamReader);

                baos = new ByteArrayOutputStream();
                XMLStreamWriter xmlStreamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(
                        new OutputStreamWriter(baos, "utf-8"));

                XmlReaderToWriter.writeAll(securityStreamReader, xmlStreamWriter);
                xmlStreamWriter.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return baos.toByteArray();

        }
    };

    static {
        Init.init();
    }

    public abstract byte[] encrypt(byte[] plaintext,
            List<QName> namesToEncrypt,
            String algorithm,
            Key encryptingKey,
            String keyTransportAlgorithm,
            PublicKey wrappingKey,
            boolean content);

    public abstract byte[] decrypt(byte[] encrypted, String algorithm,
            Key privateKey);

    public static DocumentBuilder createDocumentBuilder(
            boolean validating, boolean disAllowDocTypeDeclarations) throws ParserConfigurationException {
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        dfactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        if (disAllowDocTypeDeclarations) {
            dfactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        }
        dfactory.setValidating(validating);
        dfactory.setNamespaceAware(true);
        return dfactory.newDocumentBuilder();
    }

}
