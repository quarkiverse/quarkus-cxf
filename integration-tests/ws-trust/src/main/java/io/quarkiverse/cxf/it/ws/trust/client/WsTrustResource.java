package io.quarkiverse.cxf.it.ws.trust.client;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.ws.BindingProvider;

import org.apache.cxf.BusFactory;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.trust.STSClient;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.it.ws.trust.server.TrustHelloService;

@Path("/ws-trust")
public class WsTrustResource {

    @Inject
    @CXFClient("hello-ws-trust")
    TrustHelloService hello;

    @PostConstruct
    void init() {
        Map<String, Object> ctx = ((BindingProvider) hello).getRequestContext();
        ctx.put(SecurityConstants.STS_CLIENT, createSTSClient());
    }

    @POST
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public Response hello(String body) {
        try {
            return Response.ok(hello.hello(body)).build();
        } catch (Exception e) {
            final StringWriter w = new StringWriter();
            final PrintWriter pw = new PrintWriter(w);
            e.printStackTrace(pw);
            return Response.status(500).entity(w.toString()).build();
        }
    }

    /**
     * Create and configure an STSClient for use by service TrustHelloServiceImpl.
     *
     * Whenever an "<sp:IssuedToken>" policy is configured on a WSDL port, as is the
     * case for TrustHelloServiceImpl, a STSClient must be created and configured in
     * order for the service to connect to the STS-server to obtain a token.
     *
     * @param bus
     * @param stsWsdlLocation
     * @param stsService
     * @param stsPort
     * @return
     */
    private static STSClient createSTSClient() {
        String stsWsdlLocation = "http://localhost:8081/services/sts?wsdl";
        STSClient stsClient = new STSClient(BusFactory.getDefaultBus());
        if (stsWsdlLocation != null) {
            stsClient.setWsdlLocation(stsWsdlLocation);
            stsClient.setServiceQName(new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/", "SecurityTokenService"));
            stsClient.setEndpointQName(new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/", "UT_Port"));
        }
        Map<String, Object> props = stsClient.getProperties();
        props.put(SecurityConstants.USERNAME, "alice");
        props.put(SecurityConstants.PASSWORD, "clarinet");
        props.put(SecurityConstants.ENCRYPT_PROPERTIES,
                Thread.currentThread().getContextClassLoader().getResource("clientKeystore.properties"));
        props.put(SecurityConstants.ENCRYPT_USERNAME, "sts");
        props.put(SecurityConstants.STS_TOKEN_USERNAME, "client");
        props.put(SecurityConstants.STS_TOKEN_PROPERTIES,
                Thread.currentThread().getContextClassLoader().getResource("clientKeystore.properties"));
        props.put(SecurityConstants.STS_TOKEN_USE_CERT_FOR_KEYINFO, "true");
        return stsClient;
    }
}
