package io.quarkiverse.cxf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

public class CXFServletInfos {

    private final List<CXFServletInfo> infos;
    private static final Logger LOGGER = Logger.getLogger(CXFServletInfos.class);

    public CXFServletInfos() {
        LOGGER.warn("new CXFServletInfos");
        infos = new ArrayList<CXFServletInfo>();
    }

    public void add(CXFServletInfo info) {
        LOGGER.warn("CXFServletInfos getInstance");
        infos.add(info);
    }

    public List<CXFServletInfo> getInfos() {
        return infos;
    }

    public List<String> getWrappersclasses() {
        if (infos == null)
            return Collections.EMPTY_LIST;
        return infos.stream().map(CXFServletInfo::getWrapperClassNames).flatMap(List::stream)
                .collect(Collectors.toList());
    }
}
