package io.quarkiverse.cxf;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;
import org.wildfly.common.annotation.Nullable;

/**
 * Provides runtime metadata for a CXF client.
 *
 * <p>
 * This class contains extracted from a SEI. It contains basic data to
 * setup a proxy client for a given SEI.
 * </p>
 *
 * @author wh81752
 */
public class CXFClientData {
    private static final Logger LOGGER = Logger.getLogger(CXFClientData.class);
    final public List<String> classNames = new ArrayList<>();
    public String soapBinding;
    public String sei;
    public String wsName;
    public String wsNamespace;
    public @Nullable String serviceName;

}
