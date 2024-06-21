package io.quarkiverse.cxf.deployment.test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.xml.bind.annotation.XmlAttribute;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.CxfClientConfig;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduitFactory;
import io.quarkus.test.QuarkusUnitTest;

public class ClientHttpPolicyDefaultsTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, SlowHelloServiceImpl.class))
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor",
                    SlowHelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.client.hello.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.hello.service-interface", HelloService.class.getName());

    @CXFClient
    HelloService helloService;

    @Inject
    Logger logger;

    /**
     * Ensures that the defaults set in {@link CxfClientConfig} are the same as the effective defaults used in
     * {@link HTTPClientPolicy}.
     *
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @Test
    public void defaults() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        final Set<String> unsupportedAttributes = Set.of("asyncExecuteTimeout");
        final String configPrefix = "quarkus.cxf.client.hello.";
        final Config config = ConfigProvider.getConfig();
        HTTPClientPolicy policy = new HTTPClientPolicy();
        for (Field f : HTTPClientPolicy.class.getDeclaredFields()) {
            XmlAttribute xmlAttribute = f.getAnnotation(XmlAttribute.class);
            if (xmlAttribute != null && !unsupportedAttributes.contains(f.getName())) {
                String fieldName = f.getName();
                String key = configPrefix + camelCaseToDash(fieldName);
                try {
                    final String prefix = f.getType() == boolean.class ? "is" : "get";
                    Method m = HTTPClientPolicy.class.getDeclaredMethod(prefix + xmlAttribute.name());
                    Object expected = m.invoke(policy);
                    Object actual = config.getOptionalValue(key, f.getType()).orElse(null);
                    logger.infof("Checking key %s: %s ?= %s", key, actual, expected);
                    Assertions.assertThat(actual).isEqualTo(expected);
                } catch (NoSuchMethodException e) {
                }
            }
        }
    }

    @Test
    void defaultConduitFactory() {
        final Bus bus = BusFactory.getDefaultBus();
        final HTTPConduitFactory factory = bus.getExtension(HTTPConduitFactory.class);
        Assertions.assertThat(factory).isInstanceOf(VertxHttpClientHTTPConduitFactory.class);

        final Client client = ClientProxy.getClient(helloService);
        Assertions.assertThat(client.getConduit()).isInstanceOf(VertxHttpClientHTTPConduit.class);
    }

    private static String camelCaseToDash(String s) {
        StringBuilder parsedString = new StringBuilder(s.substring(0, 1).toLowerCase());
        for (char c : s.substring(1).toCharArray()) {
            if (Character.isUpperCase(c)) {
                parsedString.append("-").append(Character.toLowerCase(c));
            } else {
                parsedString.append(c);
            }
        }
        return parsedString.toString().toLowerCase();
    }

    @WebService
    public interface HelloService {

        @WebMethod
        String hello(String person);

    }

    @WebService(serviceName = "HelloService")
    public static class SlowHelloServiceImpl implements HelloService {

        @Override
        public String hello(String person) {
            return "Hello " + person;
        }
    }

}
