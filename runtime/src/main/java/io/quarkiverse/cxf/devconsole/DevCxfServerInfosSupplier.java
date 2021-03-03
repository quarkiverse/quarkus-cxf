package io.quarkiverse.cxf.devconsole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import io.quarkiverse.cxf.CXFServletInfo;
import io.quarkiverse.cxf.CXFServletInfos;

public class DevCxfServerInfosSupplier implements Supplier<Collection<CXFServletInfo>> {

    private static CXFServletInfos cxfServletInfos;

    @Override
    public List<CXFServletInfo> get() {
        List<CXFServletInfo> servletInfos = cxfServletInfos != null ? new ArrayList<>(cxfServletInfos.getInfos())
                : new ArrayList<>();
        servletInfos.sort(Comparator.comparing(CXFServletInfo::getSei));
        return servletInfos;
    }

    public static void setServletInfos(CXFServletInfos infos) {
        if (cxfServletInfos == null) {
            cxfServletInfos = infos;
        }
    }
}
