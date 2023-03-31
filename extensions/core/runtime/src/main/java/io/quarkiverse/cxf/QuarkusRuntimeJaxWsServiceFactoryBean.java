package io.quarkiverse.cxf;

import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.factory.ServiceConstructionException;

import io.quarkus.arc.Subclass;

public class QuarkusRuntimeJaxWsServiceFactoryBean extends JaxWsServiceFactoryBean {

    private static final Logger LOG = LogUtils.getLogger(QuarkusRuntimeJaxWsServiceFactoryBean.class);

    @Override
    public void setServiceClass(Class<?> serviceClass) {
        if (serviceClass == null) {
            Message message = new Message("SERVICECLASS_MUST_BE_SET", LOG);
            throw new ServiceConstructionException(message);
        }
        if (Subclass.class.isAssignableFrom(serviceClass)) {
            serviceClass = serviceClass.getSuperclass();
        }
        setJaxWsImplementorInfo(new JaxWsImplementorInfo(serviceClass));
        super.setServiceClass(getJaxWsImplementorInfo().getEndpointClass());
        super.setServiceType(getJaxWsImplementorInfo().getSEIType());
    }
}
