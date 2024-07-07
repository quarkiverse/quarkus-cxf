package io.quarkiverse.cxf.deployment;

import jakarta.annotation.Resource;
import jakarta.annotation.Resources;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.jandex.DotName;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.annotation.CXFEndpoint;

public class CxfDotNames {

    static final DotName CXFCLIENT_ANNOTATION = DotName.createSimple(CXFClient.class.getName());
    static final DotName CXF_ENDPOINT_ANNOTATION = DotName.createSimple(CXFEndpoint.class.getName());
    static final DotName INJECT_INSTANCE = DotName.createSimple(Instance.class.getName());
    static final DotName WEBSERVICE_ANNOTATION = DotName.createSimple("jakarta.jws.WebService");
    static final DotName WEBSERVICE_PROVIDER_ANNOTATION = DotName.createSimple("jakarta.xml.ws.WebServiceProvider");
    static final DotName WEBSERVICE_PROVIDER_INTERFACE = DotName.createSimple("jakarta.xml.ws.Provider");
    static final DotName WEBSERVICE_CLIENT = DotName.createSimple("jakarta.xml.ws.WebServiceClient");
    static final DotName BINDING_TYPE_ANNOTATION = DotName.createSimple("jakarta.xml.ws.BindingType");
    static final DotName REQUEST_WRAPPER_ANNOTATION = DotName.createSimple("jakarta.xml.ws.RequestWrapper");
    static final DotName RESPONSE_WRAPPER_ANNOTATION = DotName.createSimple("jakarta.xml.ws.ResponseWrapper");
    static final DotName JAXWS_SERVICE_CLASS = DotName.createSimple("jakarta.xml.ws.Service");
    static final DotName JAKARTA_ANNOTATION_RESOURCE = DotName.createSimple(Resource.class.getName());
    static final DotName JAKARTA_ANNOTATION_RESOURCES = DotName.createSimple(Resources.class.getName());
    static final DotName INJECT = DotName.createSimple(Inject.class.getName());
    static final DotName WEBSERVICE_CONTEXT = DotName.createSimple(jakarta.xml.ws.WebServiceContext.class.getName());
    static final DotName JAVA_LANG_OBJECT = DotName.createSimple(Object.class.getName());
}
