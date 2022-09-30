package io.quarkiverse.cxf.deployment;

import javax.enterprise.inject.Instance;

import org.jboss.jandex.DotName;

import io.quarkiverse.cxf.annotation.CXFClient;

public class CxfDotNames {

    static final DotName CXFCLIENT_ANNOTATION = DotName.createSimple(CXFClient.class.getName());
    static final DotName INJECT_INSTANCE = DotName.createSimple(Instance.class.getName());
    static final DotName WEBSERVICE_ANNOTATION = DotName.createSimple("javax.jws.WebService");
    static final DotName WEBSERVICE_PROVIDER_ANNOTATION = DotName.createSimple("javax.xml.ws.WebServiceProvider");
    static final DotName WEBSERVICE_PROVIDER_INTERFACE = DotName.createSimple("javax.xml.ws.Provider");
    static final DotName WEBSERVICE_CLIENT = DotName.createSimple("javax.xml.ws.WebServiceClient");
    static final DotName BINDING_TYPE_ANNOTATION = DotName.createSimple("javax.xml.ws.BindingType");
    static final DotName REQUEST_WRAPPER_ANNOTATION = DotName.createSimple("javax.xml.ws.RequestWrapper");
    static final DotName RESPONSE_WRAPPER_ANNOTATION = DotName.createSimple("javax.xml.ws.ResponseWrapper");
    static final DotName JAXWS_SERVICE_CLASS = DotName.createSimple("javax.xml.ws.Service");

}
