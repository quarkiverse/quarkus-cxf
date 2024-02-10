package io.quarkiverse.cxf.ws.security.sts.client;

import jakarta.enterprise.inject.Produces;

import org.apache.cxf.BusFactory;
import org.apache.cxf.ws.security.trust.STSClient;

/**
 * A subclass of {@link STSClient} with a no-args constructor to be able to use it as a CDI bean.
 * <p>
 * If you use {@link STSClient} directly as a return type of a {@link Produces} method, the CDI container will complain as
 * follows
 *
 * <pre>
 * It's not possible to automatically add a synthetic no-args constructor to an unproxyable bean class. You need to
 * manually add a non-private no-args constructor to org.apache.cxf.ws.security.trust.STSClient in order to fulfill the
 * requirements for normal scoped/intercepted/decorated beans.
 * </pre>
 *
 * @since 2.8.0
 */
public class STSClientBean extends STSClient {

    public static STSClientBean create() {
        return new STSClientBean();
    }

    protected STSClientBean() {
        super(BusFactory.getDefaultBus());
    }
}
