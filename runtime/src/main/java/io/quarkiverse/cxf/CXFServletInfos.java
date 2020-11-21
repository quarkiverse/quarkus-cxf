package io.quarkiverse.cxf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;

@Singleton
@Unremovable
public class CXFServletInfos {

    private final List<CXFServletInfo> infos;
    private static final Logger LOGGER = Logger.getLogger(CXFServletInfos.class);

    public CXFServletInfos() {
        LOGGER.warn("new CXFServletInfos");
        infos = new ArrayList<>();
    }

    public Collection<CXFServletInfo> getInfos() {
        return infos;
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
}
