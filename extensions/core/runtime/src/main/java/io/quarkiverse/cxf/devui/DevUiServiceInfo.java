package io.quarkiverse.cxf.devui;

import io.quarkiverse.cxf.CXFServletInfo;

public class DevUiServiceInfo {

    public static DevUiServiceInfo of(CXFServletInfo info) {
        return new DevUiServiceInfo(info.getPath() + info.getRelativePath(), info.getClassName());
    }

    private final String path;
    private final String implementor;

    public DevUiServiceInfo(String path, String implementor) {
        super();
        this.path = path;
        this.implementor = implementor;
    }

    public String getPath() {
        return path;
    }

    public String getImplementor() {
        return implementor;
    }

    @Override
    public String toString() {
        return "DevUiServiceInfo [path=" + path + ", implementor=" + implementor + "]";
    }

}