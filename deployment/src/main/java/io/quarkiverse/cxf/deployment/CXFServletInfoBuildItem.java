package io.quarkiverse.cxf.deployment;

import java.util.List;

public final class CXFServletInfoBuildItem extends CxfInfoBuildItem {

    private String path;
    private String className;
    private String sei;
    private String soapBinding;
    private List<String> wrapperClassNames;

    public CXFServletInfoBuildItem(String path, String className, String sei, String wsdlPath, String soapBinding,
            List<String> wrapperClassNames) {
        super(sei, wsdlPath);
        this.path = path;
        this.className = className;
        this.soapBinding = soapBinding;
        this.wrapperClassNames = wrapperClassNames;
    }

    public String getClassName() {
        return className;
    }

    public String getPath() {
        return path;
    }

    public List<String> getWrapperClassNames() {
        return wrapperClassNames;
    }

    public String getSOAPBinding() {
        return soapBinding;
    }

}
