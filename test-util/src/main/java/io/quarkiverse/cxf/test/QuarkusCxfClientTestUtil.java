package io.quarkiverse.cxf.test;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.LaunchMode;

public class QuarkusCxfClientTestUtil {

    private QuarkusCxfClientTestUtil() {
    }

    public static <T> T getClient(Class<T> serviceInterface, String path) {
        try {
            final URL serviceUrl = new URL(getServerUrl() + path + "?wsdl");
            final QName qName = new QName("http://server.it.cxf.quarkiverse.io/", serviceInterface.getSimpleName());
            final Service service = javax.xml.ws.Service.create(serviceUrl, qName);
            return service.getPort(serviceInterface);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getServerUrl() {
        Config config = ConfigProvider.getConfig();
        final int port = LaunchMode.current().equals(LaunchMode.TEST) ? config.getValue("quarkus.http.test-port", Integer.class)
                : config.getValue("quarkus.http.port", Integer.class);
        return String.format("http://localhost:%d", port);
    }

    public static String getEndpointUrl(Object port) {
        return (String) ((BindingProvider) port).getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
    }

}
