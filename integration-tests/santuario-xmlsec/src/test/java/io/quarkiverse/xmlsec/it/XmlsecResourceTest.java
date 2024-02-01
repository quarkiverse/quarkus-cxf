package io.quarkiverse.xmlsec.it;

import static io.restassured.RestAssured.given;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class XmlsecResourceTest {

    @ParameterizedTest
    @EnumSource(Encryption.class)
    public void encryptDecryptDom(Encryption encryption)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        try (InputStream plaintext = getClass().getClassLoader().getResourceAsStream("plaintext.xml");
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            IOUtils.copy(plaintext, baos);
            byte[] plainBytes = baos.toByteArray();
            byte[] encrypted = given()
                    .body(plainBytes)
                    .when()
                    .post("/xmlsec/" + encryption.name() + "/encrypt")
                    .then()
                    .statusCode(200)
                    .extract().body().asByteArray();
            try (ByteArrayInputStream in = new ByteArrayInputStream(encrypted)) {

                DocumentBuilder builder = Encryption.createDocumentBuilder(false, true);
                Document encryptedDoc = builder.parse(in);

                XPathFactory xpf = XPathFactory.newInstance();
                XPath xpath = xpf.newXPath();
                xpath.setNamespaceContext(new DSNamespaceContext());
                String expression = "//xenc:EncryptedData[1]";
                Element encElement = (Element) xpath.evaluate(expression, encryptedDoc, XPathConstants.NODE);
                Assertions.assertNotNull(encElement);

                // Check the CreditCard encrypted ok
                NodeList nodeList = encryptedDoc.getElementsByTagNameNS("urn:example:po", "CreditCard");
                Assertions.assertEquals(nodeList.getLength(), 0);

            }

            /* Decrypt */
            byte[] decrypted = given()
                    .body(encrypted)
                    .when()
                    .post("/xmlsec/" + encryption.name() + "/decrypt")
                    .then()
                    .statusCode(200)
                    .extract().body().asByteArray();
            try (ByteArrayInputStream in = new ByteArrayInputStream(decrypted)) {
                DocumentBuilder builder = Encryption.createDocumentBuilder(false, true);
                Document decryptedDoc = builder.parse(in);
                // Check the CreditCard decrypted ok
                NodeList nodeList = decryptedDoc.getElementsByTagNameNS("urn:example:po", "CreditCard");
                Assertions.assertEquals(nodeList.getLength(), 1);
            }

        }

    }

    @ParameterizedTest
    @EnumSource(Signing.class)
    public void signVerify(Signing signature)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {

        try (InputStream plaintext = getClass().getClassLoader().getResourceAsStream("plaintext.xml");
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            IOUtils.copy(plaintext, baos);
            byte[] plainBytes = baos.toByteArray();
            byte[] signed = given()
                    .body(plainBytes)
                    .when()
                    .post("/xmlsec/" + signature.name() + "/sign")
                    .then()
                    .statusCode(200)
                    .extract().body().asByteArray();
            try (ByteArrayInputStream in = new ByteArrayInputStream(signed)) {

                DocumentBuilder builder = Encryption.createDocumentBuilder(false, true);
                Document encryptedDoc = builder.parse(in);

                XPathFactory xpf = XPathFactory.newInstance();
                XPath xpath = xpf.newXPath();
                xpath.setNamespaceContext(new DSNamespaceContext());
                Element encElement = (Element) xpath.evaluate("//dsig:Signature", encryptedDoc, XPathConstants.NODE);
                Assertions.assertNotNull(encElement);

            }

            /* Verify the signature */
            given()
                    .body(signed)
                    .when()
                    .post("/xmlsec/" + signature.name() + "/verify")
                    .then()
                    .statusCode(204);

        }
    }

}
