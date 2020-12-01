package io.quarkiverse.cxf;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlSeeAlso;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;

public class QuarkusJaxWsServiceFactoryBean extends JaxWsServiceFactoryBean {

    private static final org.jboss.logging.Logger LOGGER = org.jboss.logging.Logger
            .getLogger(QuarkusJaxWsServiceFactoryBean.class);

    public QuarkusJaxWsServiceFactoryBean(List<String> classNames) {
        wrapperClasses = classNames.stream().distinct().map(className -> {
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
        wrapperClasses = null;
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
}
