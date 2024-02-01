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
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignatureProperties;
import javax.xml.crypto.dsig.SignatureProperty;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.XMLSignContext;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.XMLValidateContext;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.xml.security.Init;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.stax.ext.InboundXMLSec;
import org.apache.xml.security.stax.ext.OutboundXMLSec;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.XMLSec;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.ext.XMLSecurityProperties;
import org.apache.xml.security.stax.impl.securityToken.X509SecurityToken;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventConstants;
import org.apache.xml.security.stax.securityEvent.SecurityEventConstants.Event;
import org.apache.xml.security.stax.securityEvent.SecurityEventListener;
import org.apache.xml.security.stax.securityEvent.SignedElementSecurityEvent;
import org.apache.xml.security.stax.securityEvent.X509TokenSecurityEvent;
import org.apache.xml.security.stax.securityToken.SecurityTokenConstants;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Some utility methods for encrypting/decrypting documents
 * <p>
 * Adapted form <a href=
 * "https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureUtils.java">https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureUtils.java</a>
 * by <a href="https://github.com/coheigea">Colm O hEigeartaigh</a>
 */
public enum Signing {
    dom() {

        /**
         * Sign an XML Document using the DOM API.
         * <p>
         * Adapted form <a href=
         * "https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureDOMTest.java#L42">https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureDOMTest.java#L42</a>
         * by <a href="https://github.com/coheigea">Colm O hEigeartaigh</a>
         */
        @Override
        public byte[] sign(byte[] plaintext, Key key, X509Certificate cert, List<QName> namesToSign) {
            try (ByteArrayInputStream in = new ByteArrayInputStream(plaintext)) {
                Document document = Encryption.createDocumentBuilder(false, false).parse(in);

                XMLSignature sig = new XMLSignature(document, "", javax.xml.crypto.dsig.SignatureMethod.RSA_SHA256,
                        "http://www.w3.org/2001/10/xml-exc-c14n#");
                Element root = document.getDocumentElement();
                root.appendChild(sig.getElement());

                for (QName nameToSign : namesToSign) {
                    NodeList elementsToSign = document.getDocumentElement().getElementsByTagNameNS(nameToSign.getNamespaceURI(),
                            nameToSign.getLocalPart());
                    for (int i = 0; i < elementsToSign.getLength(); i++) {
                        Element elementToSign = (Element) elementsToSign.item(i);
                        String id = UUID.randomUUID().toString();
                        elementToSign.setAttributeNS(null, "Id", id);
                        elementToSign.setIdAttributeNS(null, "Id", true);

                        Transforms transforms = new Transforms(document);
                        transforms.addTransform("http://www.w3.org/2001/10/xml-exc-c14n#");
                        sig.addDocument("#" + id, transforms, DigestMethod.SHA256);
                    }
                }

                sig.sign(key);

                sig.addKeyInfo(cert);

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    XMLUtils.outputDOM(document, baos);
                    return baos.toByteArray();
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

        @Override
        public void verify(byte[] encrypted, X509Certificate cert) {
            verifyDom(encrypted, cert);
        }

    },
    domEnveloped() {

        /**
         * Sign an XML Document using the DOM API.
         * <p>
         * Adapted form <a href=
         * "https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureDOMEnvelopedTest.java#L50">https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureDOMEnvelopedTest.java#L50</a>
         * by <a href="https://github.com/coheigea">Colm O hEigeartaigh</a>
         */
        @Override
        public byte[] sign(byte[] plaintext, Key key, X509Certificate cert, List<QName> elementsToSign) {
            try (ByteArrayInputStream in = new ByteArrayInputStream(plaintext)) {
                Document document = Encryption.createDocumentBuilder(false, false).parse(in);

                XMLSignature sig = new XMLSignature(document, "", javax.xml.crypto.dsig.SignatureMethod.RSA_SHA256,
                        "http://www.w3.org/2001/10/xml-exc-c14n#");
                Element root = document.getDocumentElement();
                root.appendChild(sig.getElement());

                Transforms transforms = new Transforms(document);
                transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
                transforms.addTransform("http://www.w3.org/2001/10/xml-exc-c14n#");

                sig.addDocument("", transforms, DigestMethod.SHA256);

                sig.sign(key);

                if (cert != null) {
                    sig.addKeyInfo(cert);
                }

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    XMLUtils.outputDOM(document, baos);
                    return baos.toByteArray();
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

        @Override
        public void verify(byte[] encrypted, X509Certificate cert) {
            verifyDom(encrypted, cert);
        }

    },
    jsr105() {
        /**
         * Sign an XML Document using the JSR-105 API.
         * <p>
         * Adapted form <a href=
         * "https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureJSR105EnvelopedTest.java#L75">https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureJSR105EnvelopedTest.java#L75</a>
         * by <a href="https://github.com/coheigea">Colm O hEigeartaigh</a>
         */
        @Override
        public byte[] sign(byte[] plaintext, Key key, X509Certificate cert, List<QName> namesToSign) {
            try (ByteArrayInputStream in = new ByteArrayInputStream(plaintext)) {
                Document document = Encryption.createDocumentBuilder(false, false).parse(in);

                XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");
                CanonicalizationMethod c14nMethod = signatureFactory
                        .newCanonicalizationMethod("http://www.w3.org/2001/10/xml-exc-c14n#", (C14NMethodParameterSpec) null);

                KeyInfoFactory keyInfoFactory = signatureFactory.getKeyInfoFactory();
                X509Data x509Data = keyInfoFactory.newX509Data(Collections.singletonList(cert));
                javax.xml.crypto.dsig.keyinfo.KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data));

                List<javax.xml.crypto.dsig.Reference> referenceList = new ArrayList<>();
                for (QName nameToSign : namesToSign) {
                    NodeList elementsToSign = document.getDocumentElement().getElementsByTagNameNS(nameToSign.getNamespaceURI(),
                            nameToSign.getLocalPart());
                    for (int i = 0; i < elementsToSign.getLength(); i++) {
                        Element elementToSign = (Element) elementsToSign.item(i);
                        String id = UUID.randomUUID().toString();
                        elementToSign.setAttributeNS(null, "Id", id);
                        elementToSign.setIdAttributeNS(null, "Id", true);

                        javax.xml.crypto.dsig.Transform transform = signatureFactory.newTransform(
                                "http://www.w3.org/2001/10/xml-exc-c14n#", (TransformParameterSpec) null);

                        DigestMethod digestMethod = signatureFactory.newDigestMethod(DigestMethod.SHA256,
                                null);
                        javax.xml.crypto.dsig.Reference reference = signatureFactory.newReference(
                                "#" + id,
                                digestMethod,
                                Collections.singletonList(transform),
                                null,
                                null);
                        referenceList.add(reference);
                    }
                }

                SignatureMethod signatureMethod = signatureFactory.newSignatureMethod(SignatureMethod.RSA_SHA256, null);
                SignedInfo signedInfo = signatureFactory.newSignedInfo(c14nMethod, signatureMethod, referenceList);

                javax.xml.crypto.dsig.XMLSignature sig = signatureFactory.newXMLSignature(
                        signedInfo,
                        keyInfo,
                        null,
                        null,
                        null);

                XMLSignContext signContext = new DOMSignContext(key, document.getDocumentElement());
                sig.sign(signContext);

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    XMLUtils.outputDOM(document, baos);
                    return baos.toByteArray();
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

        /**
         * Verify an XML Document signature using the JSR-105 API.
         * <p>
         * Adapted form <a href=
         * "https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureJSR105Test.java#L75">https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureJSR105Test.java#L75</a>
         * by <a href="https://github.com/coheigea">Colm O hEigeartaigh</a>
         *
         */
        @Override
        public void verify(byte[] encrypted, X509Certificate cert) {
            try (ByteArrayInputStream in = new ByteArrayInputStream(encrypted)) {
                DocumentBuilder builder = Encryption.createDocumentBuilder(false, false);
                Document document = builder.parse(in);

                setIds(document.getDocumentElement());

                // Verify using DOM
                Element sigElement = getSignatureElement(encrypted, document);

                XMLValidateContext context = new DOMValidateContext(cert.getPublicKey(), sigElement);
                context.setProperty("javax.xml.crypto.dsig.cacheReference", Boolean.TRUE);
                context.setProperty("org.apache.jcp.xml.dsig.secureValidation", Boolean.TRUE);
                context.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);

                XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");
                javax.xml.crypto.dsig.XMLSignature xmlSignature = signatureFactory.unmarshalXMLSignature(context);

                // Check the Signature value
                if (!xmlSignature.validate(context)) {
                    throw new IllegalStateException("Invalid signature");
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    },
    jsr105Enveloped() {
        /**
         * Sign an XML Document using the JSR-105 API.
         * <p>
         * Adapted form <a href=
         * "https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureJSR105EnvelopedTest.java#L75">https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureJSR105EnvelopedTest.java#L75</a>
         * by <a href="https://github.com/coheigea">Colm O hEigeartaigh</a>
         */
        @Override
        public byte[] sign(byte[] plaintext, Key key, X509Certificate cert, List<QName> elementsToSign) {
            try (ByteArrayInputStream in = new ByteArrayInputStream(plaintext)) {
                Document document = Encryption.createDocumentBuilder(false, false).parse(in);

                // Sign using DOM
                XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");
                KeyInfoFactory keyInfoFactory = signatureFactory.getKeyInfoFactory();
                X509Data x509Data = keyInfoFactory.newX509Data(Collections.singletonList(cert));
                javax.xml.crypto.dsig.keyinfo.KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data));

                SignedInfo signedInfo = createSignedInfo(signatureFactory, keyInfo, signaturePropertyId);

                // Add a SignatureProperty containing a Timestamp
                Element timestamp = document.createElementNS(null, "Timestamp");
                timestamp.setTextContent(Instant.now().toString());
                XMLStructure content = new DOMStructure(timestamp);
                SignatureProperty signatureProperty = signatureFactory.newSignatureProperty(Collections.singletonList(content),
                        "#" + signatureId, signaturePropertyId);
                SignatureProperties signatureProperties = signatureFactory
                        .newSignatureProperties(Collections.singletonList(signatureProperty), null);
                XMLObject object = signatureFactory.newXMLObject(Collections.singletonList(signatureProperties), null, null,
                        null);

                javax.xml.crypto.dsig.XMLSignature sig = signatureFactory.newXMLSignature(
                        signedInfo,
                        keyInfo,
                        Collections.singletonList(object),
                        signatureId,
                        null);

                XMLSignContext signContext = new DOMSignContext(key, document.getDocumentElement());
                sig.sign(signContext);

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    XMLUtils.outputDOM(document, baos);
                    return baos.toByteArray();
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

        /**
         * Verify an XML Document signature using the JSR-105 API.
         * <p>
         * Adapted form <a href=
         * "https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureJSR105EnvelopedTest.java#L75">https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureJSR105EnvelopedTest.java#L75</a>
         * by <a href="https://github.com/coheigea">Colm O hEigeartaigh</a>
         *
         */
        @Override
        public void verify(byte[] encrypted, X509Certificate cert) {
            try (ByteArrayInputStream in = new ByteArrayInputStream(encrypted)) {
                DocumentBuilder builder = Encryption.createDocumentBuilder(false, false);
                Document document = builder.parse(in);

                // Verify using DOM
                Element sigElement = getSignatureElement(encrypted, document);

                XMLValidateContext context = new DOMValidateContext(cert.getPublicKey(), sigElement);
                context.setProperty("javax.xml.crypto.dsig.cacheReference", Boolean.TRUE);
                context.setProperty("org.apache.jcp.xml.dsig.secureValidation", Boolean.TRUE);
                context.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);

                XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");
                javax.xml.crypto.dsig.XMLSignature xmlSignature = signatureFactory.unmarshalXMLSignature(context);

                // Check the Signature value
                if (!xmlSignature.validate(context)) {
                    throw new IllegalStateException("Invalid signature");
                }

                // First find the Timestamp
                SignatureProperty timestampSignatureProperty = getTimestampSignatureProperty(xmlSignature);
                if (timestampSignatureProperty == null) {
                    throw new IllegalStateException("Null timestampSignatureProperty");
                }

                KeyInfoFactory keyInfoFactory = signatureFactory.getKeyInfoFactory();
                X509Data x509Data = keyInfoFactory.newX509Data(Collections.singletonList(cert));
                javax.xml.crypto.dsig.keyinfo.KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data));

                SignedInfo signedInfo = createSignedInfo(signatureFactory, keyInfo, signaturePropertyId);

                // Check that what was signed is what we expected to be signed.
                boolean foundEnvelopedSig = false;
                boolean foundSignedTimestamp = false;
                for (Object refObject : signedInfo.getReferences()) {
                    Reference ref = (Reference) refObject;
                    if ("".equals(ref.getURI())) {
                        List<Transform> transforms = (List<Transform>) ref.getTransforms();
                        if (transforms != null
                                && transforms.stream().anyMatch(t -> t.getAlgorithm().equals(Transform.ENVELOPED))) {
                            foundEnvelopedSig = true;
                        }
                    } else if ("http://www.w3.org/2000/09/xmldsig#SignatureProperties".equals(ref.getType())
                            && ref.getURI().equals("#" + timestampSignatureProperty.getId())) {
                        // Found matching SignatureProperties Object
                        // Now validate Timestamp
                        validateTimestamp(timestampSignatureProperty, cert);
                        foundSignedTimestamp = true;
                    }
                }
                if (sigElement.getParentNode() != document.getDocumentElement()) {
                    throw new IllegalStateException("sigElement should be the document root element");
                }
                if (!foundEnvelopedSig) {
                    throw new IllegalStateException("Not found enveloped signature");
                }
                if (!foundSignedTimestamp) {
                    throw new IllegalStateException("Not found signed timestamp");
                }

                XMLSignature signature = new XMLSignature(sigElement, "");

                // Check we have a KeyInfo
                if (signature.getKeyInfo() == null) {
                    throw new IllegalStateException("Invalid signature: no key info");
                }
                if (!signature.checkSignatureValue(cert)) {
                    throw new IllegalStateException("Invalid signature");
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    },
    stax() {
        /**
         * Sign an XML Document using the StAX API.
         * <p>
         * Adapted form <a href=
         * "https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureStAXTest.java#L39">https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureStAXTest.java#L39</a>
         * by <a href="https://github.com/coheigea">Colm O hEigeartaigh</a>
         */
        @Override
        public byte[] sign(byte[] plaintext, Key key, X509Certificate cert, List<QName> namesToSign) {
            try (ByteArrayInputStream in = new ByteArrayInputStream(plaintext)) {

                // Set up the Configuration
                XMLSecurityProperties properties = new XMLSecurityProperties();
                List<XMLSecurityConstants.Action> actions = new ArrayList<XMLSecurityConstants.Action>();
                actions.add(XMLSecurityConstants.SIGNATURE);
                properties.setActions(actions);

                properties.setSignatureAlgorithm(SignatureMethod.RSA_SHA256);
                properties.setSignatureCerts(new X509Certificate[] { cert });
                properties.setSignatureKey(key);
                properties.setSignatureKeyIdentifier(
                        SecurityTokenConstants.KeyIdentifier_X509KeyIdentifier);

                for (QName nameToSign : namesToSign) {
                    SecurePart securePart = new SecurePart(nameToSign, SecurePart.Modifier.Content);
                    properties.addSignaturePart(securePart);
                }

                OutboundXMLSec outboundXMLSec = XMLSec.getOutboundXMLSec(properties);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                XMLStreamWriter xmlStreamWriter = outboundXMLSec.processOutMessage(baos, "UTF-8");

                XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
                XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(in);

                XmlReaderToWriter.writeAll(xmlStreamReader, xmlStreamWriter);
                xmlStreamWriter.close();

                return baos.toByteArray();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

        /**
         * Verify a document using the StAX API.
         * <p>
         * Adapted form <a href=
         * "https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureStAXTest.java#L39">https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureStAXTest.java#L39</a>
         * by <a href="https://github.com/coheigea">Colm O hEigeartaigh</a>
         */
        @Override
        public void verify(byte[] encrypted, X509Certificate cert) {
            try (ByteArrayInputStream in = new ByteArrayInputStream(encrypted)) {
                // Set up the Configuration
                XMLSecurityProperties properties = new XMLSecurityProperties();
                List<XMLSecurityConstants.Action> actions = new ArrayList<XMLSecurityConstants.Action>();
                actions.add(XMLSecurityConstants.SIGNATURE);
                properties.setActions(actions);

                properties.setSignatureVerificationKey(cert.getPublicKey());

                InboundXMLSec inboundXMLSec = XMLSec.getInboundWSSec(properties);

                XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
                final XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(in);

                TestSecurityEventListener eventListener = new TestSecurityEventListener();
                XMLStreamReader securityStreamReader = inboundXMLSec.processInMessage(xmlStreamReader, null, eventListener);

                while (securityStreamReader.hasNext()) {
                    securityStreamReader.next();
                }
                xmlStreamReader.close();

                // Check that what we were expecting to be signed was actually signed
                List<SignedElementSecurityEvent> signedElementEvents = eventListener
                        .getSecurityEvents(SecurityEventConstants.SignedElement);
                if (signedElementEvents == null) {
                    throw new IllegalStateException("No SignedElement recorded");
                }

                for (QName nameToSign : XmlsecResource.PAYMENT_INFO) {
                    boolean found = false;
                    for (SignedElementSecurityEvent signedElement : signedElementEvents) {
                        if (signedElement.isSigned()
                                && nameToSign.equals(getSignedQName(signedElement.getElementPath()))) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        throw new IllegalStateException("Could not find " + nameToSign + " mong signed elements");
                    }
                }

                // Check Signing cert
                X509TokenSecurityEvent tokenEvent = (X509TokenSecurityEvent) eventListener
                        .getSecurityEvent(SecurityEventConstants.X509Token);
                if (tokenEvent == null) {
                    throw new IllegalStateException("tokenEvent is null");
                }

                if (!(tokenEvent.getSecurityToken() instanceof X509SecurityToken)) {
                    throw new IllegalStateException("tokenEvent.getSecurityToken() not an instanceof X509SecurityToken");
                }
                X509SecurityToken x509SecurityToken = (X509SecurityToken) tokenEvent.getSecurityToken();
                if (!cert.equals(x509SecurityToken.getX509Certificates()[0])) {
                    throw new IllegalStateException("Unexpected X509Certificate");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private QName getSignedQName(List<QName> qnames) {
            if (qnames == null || qnames.isEmpty()) {
                return null;
            }

            return qnames.get(qnames.size() - 1);
        }

    };

    static {
        Init.init();
    }
    private static String signatureId = "_" + UUID.randomUUID().toString();
    private static String signaturePropertyId = "_" + UUID.randomUUID().toString();

    public abstract byte[] sign(byte[] plaintext, Key key, X509Certificate cert, List<QName> elementsToSign);

    public abstract void verify(byte[] encrypted, X509Certificate cert);

    private static SignatureProperty getTimestampSignatureProperty(javax.xml.crypto.dsig.XMLSignature xmlSignature) {
        Iterator<?> objects = xmlSignature.getObjects().iterator();
        while (objects.hasNext()) {
            XMLObject object = (XMLObject) objects.next();
            for (Object objectContent : object.getContent()) {
                if (objectContent instanceof SignatureProperties) {
                    for (Object signaturePropertiesObject : ((SignatureProperties) objectContent).getProperties()) {
                        SignatureProperty signatureProperty = (SignatureProperty) signaturePropertiesObject;
                        if (("#" + xmlSignature.getId()).equals(signatureProperty.getTarget())) {
                            return signatureProperty;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static Element getSignatureElement(byte[] encrypted, Document document) {
        NodeList sigs = document.getDocumentElement().getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#",
                "Signature");
        if (sigs.getLength() == 0) {
            throw new IllegalStateException(
                    "No Signature element found in " + new String(encrypted, StandardCharsets.UTF_8));
        }
        return (Element) sigs.item(0);
    }

    /**
     * Verify an XML Document signature using the SOM API.
     * <p>
     * Adapted form <a href=
     * "https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureUtils.java#L138">https://github.com/coheigea/testcases/blob/master/apache/santuario/santuario-xml-signature/src/test/java/org/apache/coheigea/santuario/xmlsignature/SignatureUtils.java#L138</a>
     * by <a href="https://github.com/coheigea">Colm O hEigeartaigh</a>
     */
    private static void verifyDom(byte[] encrypted, X509Certificate cert) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(encrypted)) {
            DocumentBuilder builder = Encryption.createDocumentBuilder(false, false);
            Document document = builder.parse(in);

            setIds(document.getDocumentElement());

            Element sigElement = getSignatureElement(encrypted, document);
            XMLSignature signature = new XMLSignature(sigElement, "");

            // Check we have a KeyInfo
            if (signature.getKeyInfo() == null) {
                throw new IllegalStateException("Invalid signature: no key info");
            }
            if (!signature.checkSignatureValue(cert)) {
                throw new IllegalStateException("Invalid signature");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void setIds(Element element) {

        if (element.hasAttributeNS(null, "Id")) {
            element.setIdAttributeNS(null, "Id", true);
        }
        final NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                setIds((Element) node);
            }
        }

    }

    private static SignedInfo createSignedInfo(XMLSignatureFactory signatureFactory, KeyInfo keyInfo,
            String signaturePropertyId)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        CanonicalizationMethod c14nMethod = signatureFactory
                .newCanonicalizationMethod("http://www.w3.org/2001/10/xml-exc-c14n#", (C14NMethodParameterSpec) null);

        SignatureMethod signatureMethod = signatureFactory.newSignatureMethod(
                SignatureMethod.RSA_SHA256,
                null);

        Transform transform = signatureFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null);

        DigestMethod digestMethod = signatureFactory.newDigestMethod(DigestMethod.SHA256, null);
        Reference reference = signatureFactory.newReference(
                "",
                digestMethod,
                Collections.singletonList(transform),
                null,
                null);
        Transform objectTransform = signatureFactory.newTransform("http://www.w3.org/2001/10/xml-exc-c14n#",
                (TransformParameterSpec) null);

        Reference objectReference = signatureFactory.newReference(
                "#" + signaturePropertyId,
                digestMethod,
                Collections.singletonList(objectTransform),
                "http://www.w3.org/2000/09/xmldsig#SignatureProperties",
                null);
        List<Reference> references = new ArrayList<>();
        references.add(reference);
        references.add(objectReference);

        return signatureFactory.newSignedInfo(c14nMethod, signatureMethod, references);

    }

    private static void validateTimestamp(SignatureProperty signatureProperty, X509Certificate signingCert)
            throws CertificateExpiredException, CertificateNotYetValidException {
        boolean foundValidTimestamp = false;
        for (Object xmlStructure : signatureProperty.getContent()) {
            DOMStructure domStructure = (DOMStructure) xmlStructure;
            if (domStructure.getNode() != null && "Timestamp".equals(domStructure.getNode().getNodeName())) {
                String timestampVal = domStructure.getNode().getTextContent();
                signingCert.checkValidity(Date.from(Instant.parse(timestampVal)));
                foundValidTimestamp = true;
            }
        }
        if (!foundValidTimestamp) {
            throw new IllegalStateException("Did not find a valid timestamp");
        }
    }

    static class TestSecurityEventListener implements SecurityEventListener {
        private List<SecurityEvent> events = new ArrayList<SecurityEvent>();

        @Override
        public void registerSecurityEvent(SecurityEvent securityEvent)
                throws XMLSecurityException {
            events.add(securityEvent);
        }

        @SuppressWarnings("unchecked")
        public <T> T getSecurityEvent(Event securityEvent) {
            for (SecurityEvent event : events) {
                if (event.getSecurityEventType() == securityEvent) {
                    return (T) event;
                }
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        public <T> List<T> getSecurityEvents(Event securityEvent) {
            List<T> foundEvents = new ArrayList<T>();
            for (SecurityEvent event : events) {
                if (event.getSecurityEventType() == securityEvent) {
                    foundEvents.add((T) event);
                }
            }
            return foundEvents;
        }

        public List<SecurityEvent> getSecurityEvents() {
            return events;
        }
    }
}
