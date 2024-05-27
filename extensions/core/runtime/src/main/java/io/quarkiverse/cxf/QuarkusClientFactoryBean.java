package io.quarkiverse.cxf;

import java.util.Map;

import org.apache.cxf.binding.soap.SoapBindingConfiguration;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.jaxws.binding.soap.JaxWsSoapBindingConfiguration;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;

public class QuarkusClientFactoryBean extends ClientFactoryBean {
    public QuarkusClientFactoryBean(Class<?> serviceClass) {
        super(new QuarkusRuntimeJaxWsServiceFactoryBean(new JaxWsImplementorInfo(serviceClass)));
    }

    @Override
    public void setServiceClass(Class<?> serviceClass) {
        super.setServiceClass(serviceClass);
        if (((JaxWsServiceFactoryBean) this.getServiceFactory()).getJaxWsImplementorInfo() == null) {
            JaxWsImplementorInfo implInfo = new JaxWsImplementorInfo(serviceClass);
            ((JaxWsServiceFactoryBean) this.getServiceFactory()).setJaxWsImplementorInfo(implInfo);
        }

    }

    @Override
    protected SoapBindingConfiguration createSoapBindingConfig() {
        JaxWsSoapBindingConfiguration bc = new JaxWsSoapBindingConfiguration(
                (JaxWsServiceFactoryBean) this.getServiceFactory());
        if (this.transportId != null) {
            bc.setTransportURI(this.transportId);
        }

        return bc;
    }

    @Override
    public void setBindingId(String bind) {
        if (!"http://schemas.xmlsoap.org/wsdl/soap/http".equals(bind)
                && !"http://schemas.xmlsoap.org/wsdl/soap/http?mtom=true".equals(bind)) {
            if (!"http://www.w3.org/2003/05/soap/bindings/HTTP/".equals(bind)
                    && !"http://www.w3.org/2003/05/soap/bindings/HTTP/?mtom=true".equals(bind)) {
                super.setBindingId(bind);
            } else {
                super.setBindingId("http://schemas.xmlsoap.org/wsdl/soap12/");
            }
        } else {
            super.setBindingId("http://schemas.xmlsoap.org/wsdl/soap/");
        }

        if (!"http://schemas.xmlsoap.org/wsdl/soap/http".equals(bind)
                && !"http://www.w3.org/2003/05/soap/bindings/HTTP/".equals(bind)) {
            if ("http://schemas.xmlsoap.org/wsdl/soap/http?mtom=true".equals(bind)
                    || "http://www.w3.org/2003/05/soap/bindings/HTTP/?mtom=true".equals(bind)) {
                this.setBindingConfig(new JaxWsSoapBindingConfiguration((JaxWsServiceFactoryBean) this.getServiceFactory()));
                ((JaxWsSoapBindingConfiguration) this.getBindingConfig()).setMtomEnabled(true);
            }
        } else {
            this.setBindingConfig(new JaxWsSoapBindingConfiguration((JaxWsServiceFactoryBean) this.getServiceFactory()));
        }

    }

    @Override
    protected void applyProperties(Endpoint ep) {
        super.applyProperties(ep);
        Map<String, Object> props = this.getProperties();
        if (props != null) {
            Object factory = props.get("org.apache.cxf.transport.http.HTTPConduitFactory");
            if (factory != null) {
                ep.getEndpointInfo().setProperty("org.apache.cxf.transport.http.HTTPConduitFactory", factory);
            }
        }
    }

}
