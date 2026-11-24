package io.quarkiverse.cxf.it.http3;

// tag::http3.adoc[]
import jakarta.annotation.Resource;
import jakarta.jws.WebService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.handler.MessageContext;

/**
 * A {@link HelloService} telling the caller through which HTTP version the request came.
 *
 * @since 4.0.0
 */
@WebService(serviceName = "HelloService", targetNamespace = "urn:hello")
public class HelloServiceImpl implements HelloService {

    /* Injected by the JAX-WS runtime, hence non-final */
    @Resource
    WebServiceContext wsContext;

    /**
     * {@inheritDoc}
     *
     * @since 4.0.0
     */
    @Override
    public String hello(String person) {
        final HttpServletRequest request = (HttpServletRequest) wsContext.getMessageContext()
                .get(MessageContext.SERVLET_REQUEST);
        /* getProtocol() returns http/1.1, h2 or h3 */
        return "Hello " + person + " over " + request.getProtocol();
    }
}
// end::http3.adoc[]
