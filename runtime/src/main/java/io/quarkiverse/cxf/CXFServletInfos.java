package io.quarkiverse.cxf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

public class CXFServletInfos {

    private final List<CXFServletInfo> infos;
    private static final Logger LOGGER = Logger.getLogger(CXFServletInfos.class);
    private String path = null;

    public CXFServletInfos() {
        LOGGER.trace("new CXFServletInfos");
        infos = new ArrayList<>();
    }

    public Collection<CXFServletInfo> getInfos() {
        return infos;
    }

    public String getPath() {
        return path;
    }

    public List<String> getWrappersclasses() {
        if (infos == null)
            return Collections.emptyList();
        return infos.stream().map(CXFServletInfo::getWrapperClassNames).flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public void add(CXFServletInfo cfg) {
        infos.add(cfg);
    }

    public void setPath(String path) {
        this.path = path;
    }
}
