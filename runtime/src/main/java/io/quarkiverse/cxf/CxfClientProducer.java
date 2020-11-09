package io.quarkiverse.cxf;

import javax.enterprise.context.ApplicationScoped;
import javax.xml.namespace.QName;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CxfClientProducer {
    private static final Logger LOGGER = Logger.getLogger(CxfClientProducer.class);

    public CXFClientInfo getInfo() {
        return null;
    };

    public Object loadCxfClient(String sei) {

        CXFClientInfo cxfClientInfo = getInfo();
        Class<?> seiClass;
        try {
            seiClass = Class.forName(cxfClientInfo.getSei(), false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            LOGGER.error("either webservice interface (client) or implementation (server) is mandatory");
            return null;
        }

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean(new QuarkusClientFactoryBean(cxfClientInfo.getClasseNames()));

        factory.setServiceClass(seiClass);
        factory.setServiceName(new QName(cxfClientInfo.getWsNamespace(), cxfClientInfo.getWsName()));
        if (cxfClientInfo.getEpName() != "") {
            factory.setEndpointName(new QName(cxfClientInfo.getEpNamespace(), cxfClientInfo.getEpName()));
        }
        factory.setAddress(cxfClientInfo.getEndpointAddress());
        if (cxfClientInfo.getSoapBinding() != null) {
            factory.setBindingId(cxfClientInfo.getSoapBinding());
        }
        if (cxfClientInfo.getWsdlUrl() != null && !cxfClientInfo.getWsdlUrl().isEmpty()) {
            factory.setWsdlURL(cxfClientInfo.getWsdlUrl());
        }
        if (cxfClientInfo.getUsername() != "") {
            factory.setUsername(cxfClientInfo.getUsername());
        }
        if (cxfClientInfo.getPassword() != "") {
            factory.setPassword(cxfClientInfo.getPassword());
        }
        LOGGER.info("cxf client loaded for " + cxfClientInfo.getSei());
        return factory.create();
    }
}
