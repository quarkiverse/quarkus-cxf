package io.quarkiverse.cxf.it.ws.rm.server;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

public final class ConnectionHelper {

    private ConnectionHelper() {
    }

    public static void setKeepAliveConnection(Object proxy, boolean keepAlive) {
        setKeepAliveConnection(proxy, keepAlive, true);
    }

    public static void setKeepAliveConnection(Object proxy, boolean keepAlive, boolean force) {
        if (force || "HP-UX".equals(System.getProperty("os.name"))
                || "Windows XP".equals(System.getProperty("os.name"))) {
            Client client = ClientProxy.getClient(proxy);
            HTTPConduit hc = (HTTPConduit) client.getConduit();
            HTTPClientPolicy cp = hc.getClient();
            cp.setConnection(keepAlive ? ConnectionType.KEEP_ALIVE : ConnectionType.CLOSE);
        }
    }

}
