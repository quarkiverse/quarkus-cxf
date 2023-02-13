package io.quarkiverse.cxf;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Unremovable;

@ApplicationScoped
@Unremovable
public class CXFClientInfos {
    private static final Logger LOGGER = Logger.getLogger(CXFClientInfos.class);

    private final Instance<CXFClientInfo> cxfClientInfoInstances;

    public CXFClientInfos(@Any Instance<CXFClientInfo> cxfClientInfoInstances) {
        LOGGER.trace("new CXFClientInfos");
        this.cxfClientInfoInstances = cxfClientInfoInstances;
    }

    public static CXFClientInfo fromSei(String sei) {
        return Arc.container().instance(CXFClientInfos.class).get()
                .getClientInfoBySei(sei);
    }

    public CXFClientInfo getClientInfoBySei(String sei) {
        for (CXFClientInfo clientInfo : cxfClientInfoInstances) {
            if (Objects.equals(clientInfo.getSei(), sei)) {
                return clientInfo;
            }
        }
        return null;
    }
}
