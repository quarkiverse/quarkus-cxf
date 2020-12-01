package io.quarkiverse.cxf.deployment;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

public final class CxfWebServiceBuildItem extends MultiBuildItem {
    private final String sei;
    private final String soapBinding;
    private final String wsNamespace;
    private final String wsName;
    private final List<String> classNames;
    private final String implementor;

    public CxfWebServiceBuildItem(String sei, String soapBinding, String wsNamespace,
            String wsName, List<String> classNames, String implementor) {
        this.sei = sei;
        this.soapBinding = soapBinding;
        this.wsNamespace = wsNamespace;
        this.wsName = wsName;
        this.classNames = classNames;
        this.implementor = implementor;
    }

    public String getSei() {
        return sei;
    }

    public String getSoapBinding() {
        return soapBinding;
    }

    public String getWsName() {
        return wsName;
    }

    public List<String> getClassNames() {
        return classNames;
    }

    public String getWsNamespace() {
        return wsNamespace;
    }

    public String getImplementor() {
        return implementor;
    }
}
