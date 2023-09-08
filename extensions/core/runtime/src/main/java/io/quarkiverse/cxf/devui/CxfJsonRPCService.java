package io.quarkiverse.cxf.devui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.spi.CDI;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.CXFServletInfo;
import io.quarkiverse.cxf.CXFServletInfos;

public class CxfJsonRPCService {

    private static CXFServletInfos cxfServletInfos;

    public List<CXFServletInfo> getServices() {
        List<CXFServletInfo> servletInfos = cxfServletInfos != null ? new ArrayList<>(cxfServletInfos.getInfos())
                : new ArrayList<>();
        servletInfos.sort(Comparator.comparing(CXFServletInfo::getSei));
        return servletInfos;
    }

    public List<CXFClientInfo> getClients() {
        List<CXFClientInfo> clientInfos = new ArrayList<>(allClientInfos());
        clientInfos.sort(Comparator.comparing(CXFClientInfo::getSei));
        return clientInfos;
    }

    private static Collection<CXFClientInfo> allClientInfos() {
        return CDI.current().select(CXFClientInfo.class).stream().collect(Collectors.toCollection(ArrayList::new));
    }

    public static void setServletInfos(CXFServletInfos infos) {
        if (cxfServletInfos == null) {
            cxfServletInfos = infos;
        }
    }
}
