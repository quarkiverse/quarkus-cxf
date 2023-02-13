package io.quarkiverse.cxf.it.ws.mtom.awt.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

import org.jboss.logging.Logger;

@WebService(name = "ImageService", serviceName = "ImageService", endpointInterface = "io.quarkiverse.cxf.it.ws.mtom.awt.server.ImageService", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/mtom-awt")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public class ImageServiceImpl implements ImageService {

    public static final String MSG_SUCCESS = "Upload Successful";
    private static final Logger log = Logger.getLogger(ImageServiceImpl.class);

    private final Map<String, ImageData> imageRepository = new ConcurrentHashMap<>();

    @Override
    public ImageData downloadImage(String name) {
        final ImageData image = imageRepository.get(name);
        if (image == null) {
            throw new IllegalStateException("Image with name " + name + " does not exist.");
        }
        return image;
    }

    @Override
    public String uploadImage(ImageData image) {

        log.infof("Uploaded image: %s");

        if (image.getData() != null && image.getName() != null) {
            imageRepository.put(image.getName(), image);
            return MSG_SUCCESS;
        }
        throw new IllegalStateException("Illegal Data Format.");
    }

}
