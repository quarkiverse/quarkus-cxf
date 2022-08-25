package io.quarkiverse.cxf.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

/**
 * {@link BuildStep}s related to {wsdl4j:wsdl4j}
 */
class Wsdl4jProcessor {

    @BuildStep
    void reflectiveClass(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
                "javax.wsdl.Types",
                "javax.wsdl.extensions.mime.MIMEPart",
                "com.ibm.wsdl.extensions.schema.SchemaImpl",
                "com.ibm.wsdl.extensions.soap12.SOAP12AddressImpl",
                "com.ibm.wsdl.extensions.soap12.SOAP12AddressSerializer",
                "com.ibm.wsdl.extensions.soap12.SOAP12BindingImpl",
                "com.ibm.wsdl.extensions.soap12.SOAP12BindingSerializer",
                "com.ibm.wsdl.extensions.soap12.SOAP12BodyImpl",
                "com.ibm.wsdl.extensions.soap12.SOAP12BodySerializer",
                "com.ibm.wsdl.extensions.soap12.SOAP12Constants",
                "com.ibm.wsdl.extensions.soap12.SOAP12FaultImpl",
                "com.ibm.wsdl.extensions.soap12.SOAP12FaultSerializer",
                "com.ibm.wsdl.extensions.soap12.SOAP12HeaderFaultImpl",
                "com.ibm.wsdl.extensions.soap12.SOAP12HeaderImpl",
                "com.ibm.wsdl.extensions.soap12.SOAP12HeaderSerializer",
                "com.ibm.wsdl.extensions.soap12.SOAP12OperationImpl",
                "com.ibm.wsdl.extensions.soap12.SOAP12OperationSerializer"));

        reflectiveClass.produce(new ReflectiveClassBuildItem(
                false,
                false,
                "javax.wsdl.Binding",
                "javax.wsdl.BindingFault",
                "javax.wsdl.BindingInput",
                "javax.wsdl.BindingOperation",
                "javax.wsdl.BindingOutput",
                "javax.wsdl.Definition",
                "javax.wsdl.Fault",
                "javax.wsdl.Import",
                "javax.wsdl.Input",
                "javax.wsdl.Message",
                "javax.wsdl.Operation",
                "javax.wsdl.Output",
                "javax.wsdl.Part",
                "javax.wsdl.Port",
                "javax.wsdl.PortType",
                "javax.wsdl.Service",
                "javax.wsdl.Types",
                "javax.wsdl.extensions.soap.SOAPBody",
                "com.ibm.wsdl.BindingFaultImpl",
                "com.ibm.wsdl.BindingImpl",
                "com.ibm.wsdl.BindingInputImpl",
                "com.ibm.wsdl.BindingOperationImpl",
                "com.ibm.wsdl.BindingOutputImpl",
                "com.ibm.wsdl.FaultImpl",
                "com.ibm.wsdl.InputImpl",
                "com.ibm.wsdl.MessageImpl",
                "com.ibm.wsdl.OperationImpl",
                "com.ibm.wsdl.OutputImpl",
                "com.ibm.wsdl.PartImpl",
                "com.ibm.wsdl.PortImpl",
                "com.ibm.wsdl.PortTypeImpl",
                "com.ibm.wsdl.ServiceImpl",
                "com.ibm.wsdl.TypesImpl",
                "com.ibm.wsdl.extensions.soap.SOAPAddressImpl",
                "com.ibm.wsdl.extensions.soap.SOAPBindingImpl",
                "com.ibm.wsdl.extensions.soap.SOAPBodyImpl",
                "com.ibm.wsdl.extensions.soap.SOAPFaultImpl",
                "com.ibm.wsdl.extensions.soap.SOAPHeaderImpl",
                "com.ibm.wsdl.extensions.soap.SOAPOperationImpl",
                "com.ibm.wsdl.factory.WSDLFactoryImpl"));

    }

    @BuildStep
    void httpProxies(BuildProducer<NativeImageProxyDefinitionBuildItem> proxies) {
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPOperation"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPBody"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPHeader"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPAddress"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPBinding"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPFault"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPHeaderFault"));
    }

}
