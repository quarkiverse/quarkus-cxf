package io.quarkiverse.cxf.deployment;

import io.quarkiverse.cxf.CXFServletInfos;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Holds the runtime {@link CXFServletInfos} reference.
 */
public final class CXFServletInfosBuildItem extends SimpleBuildItem {
    private final RuntimeValue<CXFServletInfos> cxfServletInfos;

    public CXFServletInfosBuildItem(RuntimeValue<CXFServletInfos> cxfServletInfos) {
        super();
        this.cxfServletInfos = cxfServletInfos;
    }

    public RuntimeValue<CXFServletInfos> getCxfServletInfos() {
        return cxfServletInfos;
    }

}