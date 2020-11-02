package io.quarkiverse.cxf;

import javax.enterprise.context.ApplicationScoped;
import javax.xml.namespace.QName;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AbstractCxfClientProducer {
    private static final Logger LOGGER = Logger.getLogger(AbstractCxfClientProducer.class);

    public Object loadCxfClient(String sei, String endpointAddress, String wsdlUrl, String soapBinding,
            String wsNamespace, String wsName) {
        Class<?> seiClass;
        try {
            seiClass = Class.forName(sei, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            LOGGER.error("either webservice interface (client) or implementation (server) is mandatory");
            return null;
        }
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(seiClass);
        factory.setServiceName(new QName(wsNamespace, wsName));
        //factory.setEndpointName(new QName(wsNamespace, wsName));
        factory.setAddress(endpointAddress);
        if (soapBinding != null) {
            factory.setBindingId(soapBinding);
        }
        if (wsdlUrl != null && !wsdlUrl.isEmpty()) {
            factory.setWsdlURL(wsdlUrl);
        }
        LOGGER.info("cxf client loaded for " + sei);
        return factory.create();
    }
}
