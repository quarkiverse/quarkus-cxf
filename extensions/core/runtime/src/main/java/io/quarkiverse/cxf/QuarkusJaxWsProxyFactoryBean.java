package io.quarkiverse.cxf;

import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;

public class QuarkusJaxWsProxyFactoryBean extends JaxWsProxyFactoryBean {

    private final Class<?>[] additionalImplementingClasses;

    public QuarkusJaxWsProxyFactoryBean(ClientFactoryBean fact, Class<?>... additionalImplementingClasses) {
        super(fact);
        this.additionalImplementingClasses = additionalImplementingClasses;
    }

    @Override
    protected Class<?>[] getImplementingClasses() {
        Class<?> cls = getClientFactoryBean().getServiceClass();
        Class<?>[] result = new Class<?>[additionalImplementingClasses.length + 1];
        result[0] = cls;
        System.arraycopy(additionalImplementingClasses, 0, result, 1, additionalImplementingClasses.length);
        return result;
    }

}
