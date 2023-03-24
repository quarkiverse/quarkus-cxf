package io.quarkiverse.cxf.it.ws.mtom.awt.server;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;

import javax.imageio.ImageIO;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkiverse.cxf.annotation.CXFClient;

@Path("/mtom-awt-rest")
@ApplicationScoped
public class MtomAwtResource {

    public enum ClientKey {
        imageServiceClient,
        imageServiceClientWithWrappers
    }

    @Inject
    @CXFClient("imageServiceClient")
    ImageService imageServiceClient;

    @Inject
    @CXFClient("imageServiceClientWithWrappers")
    ImageServiceWithWrappers imageServiceClientWithWrappers;

    @Path("/image/{clientKey}/{imageName}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    public Response upload(@PathParam("clientKey") ClientKey clientKey, @PathParam("imageName") String imageName,
            InputStream in) throws Exception {
        String response = null;
        switch (clientKey) {
            case imageServiceClient:
                response = imageServiceClient.uploadImage(ImageIO.read(in), imageName);
                break;
            case imageServiceClientWithWrappers:
                response = imageServiceClientWithWrappers.uploadImage(ImageIO.read(in), imageName);
                break;
            default:
                throw new IllegalStateException("Unexpected " + ClientKey.class.getName() + ": " + clientKey.name());
        }
        return Response
                .created(new URI("https://quarkus.io/"))
                .entity(response)
                .build();
    }

    @Path("/image/{clientKey}/{imageName}")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    public byte[] download(@PathParam("clientKey") ClientKey clientKey, @PathParam("imageName") String imageName)
            throws Exception {
        java.awt.Image image = null;
        switch (clientKey) {
            case imageServiceClient:
                image = imageServiceClient.downloadImage(imageName);
                break;
            case imageServiceClientWithWrappers:
                image = imageServiceClientWithWrappers.downloadImage(imageName);
                break;
            default:
                throw new IllegalStateException("Unexpected " + ClientKey.class.getName() + ": " + clientKey.name());
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write((BufferedImage) image, "png", baos);
            return baos.toByteArray();
        }
    }

}
