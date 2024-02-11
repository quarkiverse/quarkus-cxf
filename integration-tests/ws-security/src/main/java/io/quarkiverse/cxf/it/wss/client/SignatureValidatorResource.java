package io.quarkiverse.cxf.it.wss.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.bsp.BSPEnforcer;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Path("/cxf/signature-validator")
public class SignatureValidatorResource {

    private static final String TRUSTSTORE_RESOURCE = "saml-keystore.jks";

    public final BSPEnforcer enforcer;
    public final WSSConfig wssConfig;
    public final Crypto crypto;

    public SignatureValidatorResource() {
        java.nio.file.Path trustStoreFile = null;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(TRUSTSTORE_RESOURCE)) {
            trustStoreFile = Files.createTempFile("saml-keystore-", ".jks");
            Files.createDirectories(trustStoreFile.getParent());
            Files.copy(in, trustStoreFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not copy the classpath resource " + TRUSTSTORE_RESOURCE + " to " + trustStoreFile, e);
        }

        Properties properties = new Properties();
        properties.setProperty("org.apache.ws.security.crypto.provider", "org.apache.ws.security.components.crypto.Merlin");
        properties.put("org.apache.ws.security.crypto.merlin.truststore.type", "JKS");
        properties.put("org.apache.ws.security.crypto.merlin.truststore.password", "changeit");
        properties.put("org.apache.ws.security.crypto.merlin.truststore.file", trustStoreFile.toString());
        properties.put("org.apache.ws.security.crypto.merlin.load.cacerts", "false");

        this.enforcer = new BSPEnforcer();
        this.wssConfig = WSSConfig.getNewInstance();
        try {
            this.crypto = CryptoFactory.getInstance(properties);
        } catch (WSSecurityException e) {
            throw new RuntimeException("Could not create Crypto", e);
        }
    }

    @POST
    @Path("/validate")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean validate(@QueryParam("samlNamespace") String samlNamespace, InputStream body) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document bodyDocument = builder.parse(body);

        Element element = bodyDocument.getDocumentElement();
        NodeList nodes = element.getElementsByTagNameNS(WSS4JConstants.WSSE_NS, WSS4JConstants.WSSE_LN);
        Element wsseHeader = (Element) nodes.item(0);
        if (wsseHeader == null) {
            throw new SecurityException("Assertion missing in request!");
        }

        NodeList timestampNodes = wsseHeader.getElementsByTagNameNS(WSS4JConstants.WSU_NS,
                WSS4JConstants.TIMESTAMP_TOKEN_LN);
        NodeList samlNodes = wsseHeader.getElementsByTagNameNS(samlNamespace, WSS4JConstants.ASSERTION_LN);
        NodeList binaryNodes = wsseHeader.getElementsByTagNameNS(WSS4JConstants.WSSE_NS, WSS4JConstants.BINARY_TOKEN_LN);
        NodeList signatureNodes = wsseHeader.getElementsByTagNameNS(XMLSignature.XMLNS, WSS4JConstants.SIG_LN);
        NodeList tokenRefs = wsseHeader.getElementsByTagNameNS(WSS4JConstants.WSSE_NS, "SecurityTokenReference");
        if (isEmpty(timestampNodes, samlNodes, binaryNodes, signatureNodes)) {
            throw new SecurityException("Assertion missing in request!");
        }

        return validateSecurityHeader(
                timestampNodes.item(0),
                samlNodes.item(0),
                binaryNodes.item(0),
                signatureNodes.item(0),
                toList(tokenRefs));
    }

    private List<Node> toList(NodeList nodeList) {
        final int cnt = nodeList.getLength();
        if (cnt == 0) {
            return Collections.emptyList();
        }
        final List<Node> res = new ArrayList<>(cnt);
        for (int i = 0; i < cnt; i++) {
            res.add(nodeList.item(i));
        }
        return res;
    }

    private boolean isEmpty(NodeList... nodeLists) {
        for (NodeList nl : nodeLists) {
            if (nl.getLength() == 0) {
                return true;
            }
        }
        return false;
    }

    private boolean validateSecurityHeader(
            Node timestampNode,
            Node samlNode,
            Node binaryNode,
            Node signatureNode, List<Node> additionalSignedNodes) throws Exception {

        InputStream in = new ByteArrayInputStream(Base64.getMimeDecoder().decode(binaryNode.getTextContent()));
        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);

        List<Node> signedNodes = new ArrayList<>();
        signedNodes.add(timestampNode);
        signedNodes.add(samlNode);
        signedNodes.addAll(additionalSignedNodes);

        return validateSignature(certificate, signatureNode, signedNodes);
    }

    private boolean validateSignature(
            X509Certificate certificate,
            Node signatureNode,
            List<Node> additionalNodes) throws Exception {

        DOMValidateContext valContext = new DOMValidateContext(certificate.getPublicKey(), signatureNode);

        // disable secure validation since we use SHA-1 algorithm for signature
        valContext.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.FALSE);
        for (Node node : additionalNodes) {
            Element el = (Element) node;
            if (el.hasAttributeNS(WSS4JConstants.WSU_NS, "Id")) {
                valContext.setIdAttributeNS(el, WSS4JConstants.WSU_NS, "Id");
            } else if (el.hasAttribute("ID")) {
                valContext.setIdAttributeNS(el, null, "ID");
            }
        }

        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        XMLSignature signature = fac.unmarshalXMLSignature(valContext);
        return signature.validate(valContext);
    }

}
