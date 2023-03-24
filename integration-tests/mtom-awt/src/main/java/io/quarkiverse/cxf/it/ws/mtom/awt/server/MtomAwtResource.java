package io.quarkiverse.cxf.it.ws.mtom.awt.server;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
