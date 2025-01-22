package io.quarkiverse.cxf.it.vertx.async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.deployment.test.HelloService;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;

@Path("/RestAsyncWithWsdlWithEagerInit")
public class RestAsyncWithWsdlWithEagerInit {

    @ConfigProperty(name = "quarkus.http.test-port")
    int testPort;

    @CXFClient("helloWithWsdlWithEagerInit")
    /* Use instance to prevent initializing before the local HTTP endpoint is available */
    Instance<HelloService> helloWithWsdlWithEagerInitInst;
    HelloService helloWithWsdlWithEagerInit;

    void init(@Observes StartupEvent start) {
        /*
         * We need to delay the initialization, because helloWithWsdlWithEagerInit
         * points at the local service that is not exposed yet when this handler is triggered
         */
        new Thread(() -> {
            Log.infof("Waiting for the application to open port " + testPort);
            while (true) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress("localhost", testPort), 200);
                    break;
                } catch (IOException e) {
                }
            }
            helloWithWsdlWithEagerInit = helloWithWsdlWithEagerInitInst.get();
            Log.infof("Initializing helloWithWsdlWithEagerInit eagerly: %s", helloWithWsdlWithEagerInit.hello("foo"));
        }, "await-application-readiness")
                .start();
    }

    @Path("/helloWithWsdlWithEagerInit")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> helloWithWsdlWithEagerInit(String person) {
        while (helloWithWsdlWithEagerInit == null) {
            /* Spin until the client is ready */
        }
        /* We have triggered the initialization of helloWithWsdlWithEagerInit in init() above so it should work */
        return Uni.createFrom()
                .future(helloWithWsdlWithEagerInit.helloAsync(person))
                .map(helloResponse -> helloResponse.getReturn());
    }
}
