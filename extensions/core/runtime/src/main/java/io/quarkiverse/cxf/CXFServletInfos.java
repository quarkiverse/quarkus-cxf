package io.quarkiverse.cxf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

public class CXFServletInfos {

    private final List<CXFServletInfo> infos;
    private static final Logger LOGGER = Logger.getLogger(CXFServletInfos.class);
    private final String path;
    private final String contextPath;

    public CXFServletInfos(String path, String contextPath) {
        LOGGER.trace("new CXFServletInfos");
        this.path = path;
        this.contextPath = contextPath;
        infos = new ArrayList<>();
    }

    public Collection<CXFServletInfo> getInfos() {
        return infos;
    }

    public String getPath() {
        return path;
    }

    public String getContextPath() {
        return contextPath;
    }

    public Set<String> getWrappersclasses() {
        if (infos == null)
            return Collections.emptySet();
        return infos.stream()
                .map(CXFServletInfo::getWrapperClassNames)
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void add(CXFServletInfo cfg) {
        infos.add(cfg);
    }

}
