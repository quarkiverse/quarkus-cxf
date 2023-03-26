package io.quarkiverse.cxf.deployment;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;

public class QuarkusJaxWsServiceFactoryBean extends JaxWsServiceFactoryBean {
    public Set<String> getWrappersClassNames() {
        /*
         * Because JaxWsServiceFactoryBean.wrapperClasses is not accessible, we have to simulate
         * the way how it is assembled in
         * org.apache.cxf.jaxws.WrapperClassGenerator.generate(JaxWsServiceFactoryBean, InterfaceInfo, boolean)
         */
        InterfaceInfo interfaceInfo = getService().getServiceInfos().get(0).getInterface();
        Set<String> wrapperBeans = new LinkedHashSet<>();
        for (OperationInfo opInfo : interfaceInfo.getOperations()) {
            if (opInfo.isUnwrappedCapable()) {
                Method method = (Method) opInfo.getProperty(ReflectionServiceFactoryBean.METHOD);
                if (method == null) {
                    continue;
                }
                {
                    final MessagePartInfo inf = opInfo.getInput().getFirstMessagePart();
                    if (inf.getTypeClass() != null) {
                        wrapperBeans.add(inf.getTypeClass().getName());
                    }
                }
                MessageInfo messageInfo = opInfo.getUnwrappedOperation().getOutput();
                if (messageInfo != null) {
                    final MessagePartInfo inf = opInfo.getOutput().getFirstMessagePart();
                    if (inf.getTypeClass() != null) {
                        wrapperBeans.add(inf.getTypeClass().getName());
                    }
                }
            }
        }
        return wrapperBeans;
    }
}
