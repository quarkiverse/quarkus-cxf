package io.quarkiverse.cxf.it.ws.mtom.awt.server;

import java.awt.Image;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jws.WebService;

import org.jboss.logging.Logger;

@WebService(name = "ImageService", serviceName = "ImageService", endpointInterface = "io.quarkiverse.cxf.it.ws.mtom.awt.server.ImageService", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/mtom-awt")
public class ImageServiceImpl implements ImageService {

    public static final String MSG_SUCCESS = "Upload Successful";
    private static final Logger log = Logger.getLogger(ImageServiceImpl.class);

    private final Map<String, Image> imageRepository = new ConcurrentHashMap<>();

    @Override
    public Image downloadImage(String name) {
        final Image image = imageRepository.get(name);
        if (image == null) {
            throw new IllegalStateException("Image with name " + name + " does not exist.");
        }
        log.infof("Downloading image: %s", name);
        return image;
    }

    @Override
    public String uploadImage(Image data, String name) {

        log.infof("Uploaded image: %s", name);

        if (data != null && name != null) {
            imageRepository.put(name, data);
            return MSG_SUCCESS;
        }
        throw new IllegalStateException("Illegal Data Format.");
    }

}
