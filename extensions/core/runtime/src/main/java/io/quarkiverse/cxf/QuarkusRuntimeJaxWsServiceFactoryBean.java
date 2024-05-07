package io.quarkiverse.cxf;

import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;

public class QuarkusRuntimeJaxWsServiceFactoryBean extends JaxWsServiceFactoryBean {

    public QuarkusRuntimeJaxWsServiceFactoryBean(JaxWsImplementorInfo jaxWsImplementorInfo) {
        super(jaxWsImplementorInfo);
    }

}
