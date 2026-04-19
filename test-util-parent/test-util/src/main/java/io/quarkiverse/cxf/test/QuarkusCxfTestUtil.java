package io.quarkiverse.cxf.test;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.jws.WebService;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;

import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Test utilities intended for developers implementing applications on top of Quarkus CXF.
 *
 * @since 3.33.0
 */
public class QuarkusCxfTestUtil {
    private QuarkusCxfTestUtil() {
    }

    /**
     * A shorthand for {@link #getClient(String, Class, String) getClient(getTargetNamespace(serviceInterface),
     * serviceInterface, path)}.
     *
     * @param <T> the type of the service endpoint interface (SEI)
     * @param serviceInterface the type of the service endpoint interface (SEI)
     * @param path the path to append after the URL base returned by {@link #getServerUrl()}
     * @return a ready to use SOAP client
     *
     * @since 3.33.0
     */
    public static <T> T getClient(Class<T> serviceInterface, String path) {
        return getClient(getTargetNamespace(serviceInterface), serviceInterface, path);
    }

    /**
     * @param <T> the type of the service endpoint interface (SEI)
     * @param targetNamespace the target namespace of the given {@code serviceInterface}
     * @param serviceInterface the type of the service endpoint interface (SEI)
     * @param path the path to append after the URL base returned by {@link #getServerUrl()}
     * @return a ready to use SOAP client
     *
     * @since 3.33.0
     */
    public static <T> T getClient(String targetNamespace, Class<T> serviceInterface, String path) {
        try {
            final URL serviceUrl = new URL(getServerUrl() + path + "?wsdl");
            final QName qName = new QName(targetNamespace, serviceInterface.getSimpleName());
            final Service service = Service.create(serviceUrl, qName);
            return service.getPort(serviceInterface);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the URL of the server started for a test annotated with
     *         {@code io.quarkus.test.junit.QuarkusTest} or {@code @io.quarkus.test.junit.QuarkusIntegrationTest}.
     *
     * @since 3.33.0
     */
    public static String getServerUrl() {
        final int port = ConfigProvider.getConfig().getValue("quarkus.http.port", Integer.class);
        return String.format("http://localhost:%d", port);
    }

    /**
     * Return the URL of the SOAP service the given {@code soapClient} has configured.
     *
     * @param soapClient the SOAP client proxy whose endpoint URL should be looked up
     * @return the URL of the SOAP service the given {@code soapClient} has configured
     *
     * @since 3.33.0
     */
    public static String getEndpointUrl(Object soapClient) {
        return (String) ((BindingProvider) soapClient).getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
    }

    /**
     * Get the value of {@link WebService#targetNamespace() @WebService(targetNamespace="...")} if available on the given
     * {@code serviceInterface} or the default namespace as assigned by Quarkus CXF consisting of {@code serviceInterface}'s
     * package segments
     * in reverse order.
     *
     * @param serviceInterface the SEI interface target namespace of which should be determined
     * @return the target namespace URI as a {@link String}
     *
     * @since 3.33.0
     */
    public static String getTargetNamespace(Class<?> serviceInterface) {
        WebService wsAnnotation = serviceInterface.getAnnotation(WebService.class);
        if (wsAnnotation != null && wsAnnotation.targetNamespace() != null && !wsAnnotation.targetNamespace().isEmpty()) {
            return wsAnnotation.targetNamespace();
        }

        String pkg = serviceInterface.getName();
        int lastDotPos = pkg.lastIndexOf('.');
        if (lastDotPos != -1 && lastDotPos < pkg.length() - 1) {
            pkg = pkg.substring(0, lastDotPos);
        }
        String[] segments = pkg.split("\\.");
        StringBuilder b = new StringBuilder("http://");
        for (int i = segments.length - 1; i >= 0; i--) {
            if (i != segments.length - 1) {
                b.append(".");
            }
            b.append(segments[i]);
        }
        b.append("/");
        return b.toString();
    }

}
