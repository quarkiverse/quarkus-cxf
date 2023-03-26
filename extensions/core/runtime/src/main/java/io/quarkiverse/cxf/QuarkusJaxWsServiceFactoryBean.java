package io.quarkiverse.cxf;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.xml.bind.annotation.XmlSeeAlso;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.factory.ServiceConstructionException;

import io.quarkus.arc.Subclass;

public class QuarkusJaxWsServiceFactoryBean extends JaxWsServiceFactoryBean {

    private static final org.jboss.logging.Logger LOGGER = org.jboss.logging.Logger
            .getLogger(QuarkusJaxWsServiceFactoryBean.class);

    public QuarkusJaxWsServiceFactoryBean(Set<String> wrapperClassNames) {
        wrapperClasses = wrapperClassNames.stream().map(className -> {
            try {
                return Thread.currentThread().getContextClassLoader().loadClass(className);
            } catch (ClassNotFoundException e) {
                //silent fail
            }
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                LOGGER.error("Generated Wrapper class not found", e);
            }

            return null;
        }).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Set<Class<?>> wrapperClasses;
    private static final Logger LOG = LogUtils.getLogger(QuarkusJaxWsServiceFactoryBean.class);

    @Override
    public void reset() {
        super.reset();
        //wrapperClasses = null;
    }

    @Override
    protected Set<Class<?>> getExtraClass() {
        Set<Class<?>> classes = new HashSet<>();
        if (wrapperClasses != null) {
            classes.addAll(wrapperClasses);
        }

        XmlSeeAlso xmlSeeAlsoAnno = getServiceClass().getAnnotation(XmlSeeAlso.class);

        if (xmlSeeAlsoAnno != null && xmlSeeAlsoAnno.value() != null) {
            for (int i = 0; i < xmlSeeAlsoAnno.value().length; i++) {
                Class<?> value = xmlSeeAlsoAnno.value()[i];
                if (value == null) {
                    LOG.log(Level.WARNING, "XMLSEEALSO_NULL_CLASS",
                            new Object[] { getServiceClass().getName(), i });
                } else {
                    classes.add(value);
                }

            }
        }
        return classes;
    }

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
