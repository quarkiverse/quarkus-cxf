package io.quarkiverse.cxf.it.security.policy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.it.security.policy.BeanProducers.MessageCollector;
import io.quarkiverse.cxf.it.security.policy.BeanProducers.RecordingReplayCache;

@Path("/cxf/security-policy")
public class SecurityPolicyResource {

    @Inject
    @CXFClient("hello")
    HelloService hello;

    @Inject
    @CXFClient("helloCustomHostnameVerifier")
    HelloService helloCustomHostnameVerifier;

    @Inject
    @CXFClient("helloAllowAll")
    HelloService helloAllowAll;

    @Inject
    @CXFClient("helloIp")
    HelloService helloIp;

    @Inject
    @CXFClient("helloHttps")
    HttpsPolicyHelloService helloHttps;

    @Inject
    @CXFClient("helloHttpsPkcs12")
    HttpsPolicyHelloService helloHttpsPkcs12;

    @Inject
    @CXFClient("helloHttp")
    HelloService helloHttp;

    @Inject
    @CXFClient("helloCustomEncryptSign")
    CustomEncryptSignPolicyHelloService helloCustomEncryptSign;

    @Inject
    @CXFClient("helloCustomEncryptSignWrong1")
    CustomEncryptSignWrong1PolicyHelloService helloCustomEncryptSignWrong1;

    @Inject
    @CXFClient("helloCustomEncryptSignWrong2")
    CustomEncryptSignWrong2PolicyHelloService helloCustomEncryptSignWrong2;

    @Inject
    @CXFClient("helloCustomizedEncryptSign")
    CustomizedEncryptSignPolicyHelloService helloCustomizedEncryptSign;

    @Inject
    @CXFClient("helloUsernameToken")
    UsernameTokenPolicyHelloService helloUsernameToken;

    @Inject
    @CXFClient("helloUsernameTokenAlt")
    UsernameTokenPolicyHelloService helloUsernameTokenAlt;

    @Inject
    @CXFClient("helloUsernameTokenNoMustUnderstand")
    UsernameTokenPolicyHelloService helloUsernameTokenNoMustUnderstand;

    @Inject
    @CXFClient("helloNoUsernameToken")
    HelloService helloNoUsernameToken;

    @Inject
    @CXFClient("helloEncryptSign")
    EncryptSignPolicyHelloService helloEncryptSign;

    @Inject
    @CXFClient("helloEncryptSignCrypto")
    EncryptSignPolicyHelloService helloEncryptSignCrypto;

    @Inject
    @CXFClient("helloSaml1")
    Saml1PolicyHelloService helloSaml1;

    @Inject
    @CXFClient("helloSaml2")
    Saml2PolicyHelloService helloSaml2;

    @Inject
    @Named("messageCollector")
    MessageCollector messageCollector;

    @Inject
    @Named("recordingReplayCache")
    RecordingReplayCache recordingReplayCache;

    @GET
    @Path("/drainMessages")
    @Produces(MediaType.TEXT_PLAIN)
    public String drainMessages() {
        return messageCollector.drainMessages().stream().collect(Collectors.joining("|||"));
    }

    @GET
    @Path("/drainCache")
    @Produces(MediaType.TEXT_PLAIN)
    public String drainCache() {
        return recordingReplayCache.drainEntries().stream().collect(Collectors.joining("|||"));
    }

    @GET
    @Path("/isfips")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isfips() {
        return java.security.Security.getProvider("SunPKCS11-NSS-FIPS") != null;

    }

    @POST
    @Path("/{client}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response hello(@PathParam("client") String client, String body) {
        final AbstractHelloService service;
        switch (client) {
            case "hello":
                service = hello;
                break;
            case "helloAllowAll":
                service = helloAllowAll;
                break;
            case "helloCustomHostnameVerifier":
                service = helloCustomHostnameVerifier;
                break;
            case "helloIp":
                service = helloIp;
                break;
            case "helloHttps":
                service = helloHttps;
                break;
            case "helloHttpsPkcs12":
                service = helloHttpsPkcs12;
                break;
            case "helloHttp":
                service = helloHttp;
                break;
            case "helloUsernameToken":
                service = helloUsernameToken;
                break;
            case "helloUsernameTokenAlt":
                service = helloUsernameTokenAlt;
                break;
            case "helloUsernameTokenNoMustUnderstand":
                service = helloUsernameTokenNoMustUnderstand;
                break;
            case "helloNoUsernameToken":
                service = helloNoUsernameToken;
                break;
            case "helloEncryptSign":
                service = helloEncryptSign;
                break;
            case "helloEncryptSignCrypto":
                service = helloEncryptSignCrypto;
                break;
            case "helloSaml1":
                service = helloSaml1;
                break;
            case "helloSaml2":
                service = helloSaml2;
                break;
            case "helloCustomizedEncryptSign":
                service = helloCustomizedEncryptSign;
                break;
            case "helloCustomEncryptSign":
                service = helloCustomEncryptSign;
                break;
            case "helloCustomEncryptSignWrong1":
                service = helloCustomEncryptSignWrong1;
                break;
            case "helloCustomEncryptSignWrong2":
                service = helloCustomEncryptSignWrong2;
                break;
            default:
                throw new IllegalStateException("Unexpected client " + client);
        }
        try {
            return Response.ok(service.hello(body)).build();
        } catch (Exception e) {
            final StringWriter w = new StringWriter();
            final PrintWriter pw = new PrintWriter(w);
            e.printStackTrace(pw);
            return Response.status(500).entity(w.toString()).build();
        }
    }
}
