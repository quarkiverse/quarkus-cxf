package io.quarkiverse.cxf;

import jakarta.annotation.Resource;
import jakarta.annotation.Resources;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.xml.ws.WebServiceContext;

import org.apache.cxf.jaxws.context.WebServiceContextImpl;

/**
 * Produces {@link WebServiceContext} beans to be injected to fieds and methods annotated with {@link Resource} or
 * {@link Resources}. See also io.quarkiverse.cxf.deployment.WebServiceContextProcessor#addInjectForResource().
 */
public class WebServiceContextProducer {

    @Produces
    @RequestScoped
    public WebServiceContext newWebServiceContext() {
        return new WebServiceContextImpl();
    }

}
