package io.quarkiverse.cxf.it.ws.mtom.awt.server;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.it.ws.mtom.awt.server.MtomAwtResource.ClientKey;
import io.quarkiverse.cxf.it.ws.mtom.awt.server.wrappers.ImageServiceWithWrappersImpl;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
class MtomAwtTest {

    @Test
    public void uploadDownloadMtomWithWrappers() throws IOException {
        assertUploadDownload(ClientKey.imageServiceClientWithWrappers);
    }

    @Test
    public void uploadDownloadMtom() throws IOException {
        assertUploadDownload(ClientKey.imageServiceClient);
    }

    public void assertUploadDownload(ClientKey clientKey) throws IOException {
        byte[] imageBytes = MtomAwtTest.class.getClassLoader().getResourceAsStream("linux-image.png").readAllBytes();
        String imageName = "linux-image-name";
        RestAssured.given()
                .contentType(ContentType.BINARY)
                .body(imageBytes)
                .post("/mtom-awt-rest/image/" + clientKey + "/" + imageName)
                .then()
                .statusCode(201)
                .body(CoreMatchers.equalTo(ImageServiceWithWrappersImpl.MSG_SUCCESS));

        byte[] downloadedImageBytes = RestAssured.given()
                .get("/mtom-awt-rest/image/" + clientKey + "/" + imageName)
                .then()
                .statusCode(200)
                .extract().asByteArray();

        try (ByteArrayInputStream imageBais = new ByteArrayInputStream(
                imageBytes); ByteArrayInputStream downloadedImageBais = new ByteArrayInputStream(downloadedImageBytes)) {
            Assertions.assertTrue(bufferedImagesEqual(ImageIO.read(imageBais),
                    ImageIO.read(downloadedImageBais)), "Uploaded image should match downloaded");
        }
    }

    // copied from https://stackoverflow.com/a/15305092
    boolean bufferedImagesEqual(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() == img2.getWidth() && img1.getHeight() == img2.getHeight()) {
            for (int x = 0; x < img1.getWidth(); x++) {
                for (int y = 0; y < img1.getHeight(); y++) {
                    if (img1.getRGB(x, y) != img2.getRGB(x, y))
                        return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }
}
