package io.quarkiverse.cxf.it.ws.trust.client;

import java.util.Map;

import javax.xml.namespace.QName;

//tag::ws-trust-usage.adoc-sts-client[]
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.apache.cxf.ws.security.SecurityConstants;

import io.quarkiverse.cxf.ws.security.sts.client.STSClientBean;

public class BeanProducers {

    /**
     * Create and configure an STSClient for use by the TrustHelloService client.
     */
    @Produces
    @ApplicationScoped
    @Named("stsClientBean")
    STSClientBean createSTSClient() {
        /*
         * We cannot use org.apache.cxf.ws.security.trust.STSClient as a return type of this bean producer method
         * because it does not have a no-args constructor. STSClientBean is a subclass of STSClient having one.
         */
        STSClientBean stsClient = STSClientBean.create();
        stsClient.setWsdlLocation("http://localhost:8081/services/sts?wsdl");
        stsClient.setServiceQName(new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/", "SecurityTokenService"));
        stsClient.setEndpointQName(new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/", "UT_Port"));
        Map<String, Object> props = stsClient.getProperties();
        props.put(SecurityConstants.USERNAME, "client");
        props.put(SecurityConstants.PASSWORD, "password");
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
//tag::ws-trust-usage.adoc-sts-client[]
