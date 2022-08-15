package io.quarkiverse.it.cxf;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.LaunchMode;

public class ClientTestUtil {

    private ClientTestUtil() {
    }

    public static <T> T getClient(Class<T> serviceInterface) {
        try {
            final URL serviceUrl = new URL(getServerUrl() + "/soap/greeting?wsdl");
            final Service service = javax.xml.ws.Service.create(serviceUrl,
                    new QName("http://cxf.it.quarkiverse.io/", serviceInterface.getSimpleName()));
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

}
