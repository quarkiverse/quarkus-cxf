package io.quarkiverse.cxf;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Unremovable;

@ApplicationScoped
@Unremovable
public class CXFClientInfos {
    private static final Logger LOGGER = Logger.getLogger(CXFRecorder.class);

    private final Instance<CXFClientInfo> cxfClientInfos;

    public CXFClientInfos(@Any Instance<CXFClientInfo> cxfClientInfos) {
        LOGGER.warn("new CXFClientInfos");
        this.cxfClientInfos = cxfClientInfos;
    }

    public static CXFClientInfo fromSei(String sei) {
        return Arc.container().instance(CXFClientInfos.class).get()
                .getClientInfoBySei(sei);
    }

    public CXFClientInfo getClientInfoBySei(String sei) {
        for (CXFClientInfo clientInfo : cxfClientInfos) {
            if (clientInfo.getSei() == sei) {
                return clientInfo;
            }
        }
        return null;
    }
}
