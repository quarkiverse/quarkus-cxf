package io.quarkiverse.cxf.deployment;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;

public class QuarkusJaxWsServiceFactoryBean extends JaxWsServiceFactoryBean {
    public List<String> getWrappersClassNames() {
        return getExtraClass().stream().map(Class::getCanonicalName).collect(Collectors.toList());
    }
}
