package io.quarkiverse.cxf.it.ws.rm.server;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.apache.cxf.ws.rm.RM11Constants;
import org.apache.cxf.ws.rm.feature.RMFeature;
import org.apache.cxf.ws.rmp.v200502.RMAssertion;

public class ServiceBeanProducers {

    @Produces
    @ApplicationScoped
    @Named
    RMFeature rmFeatureService() {
        return rmFeature(60000, 2000);
    }

    public static RMFeature rmFeature(long baseRetransmissionIntervalMs, long acknowledgementIntervalMs) {
        RMFeature rmFeature = new RMFeature();
        rmFeature.setRMNamespace(RM11Constants.NAMESPACE_URI);
        RMAssertion.BaseRetransmissionInterval baseRetransmissionInterval = new RMAssertion.BaseRetransmissionInterval();
        baseRetransmissionInterval.setMilliseconds(Long.valueOf(baseRetransmissionIntervalMs));
        RMAssertion.AcknowledgementInterval acknowledgementInterval = new RMAssertion.AcknowledgementInterval();
        acknowledgementInterval.setMilliseconds(Long.valueOf(acknowledgementIntervalMs));

        RMAssertion rmAssertion = new RMAssertion();
        rmAssertion.setAcknowledgementInterval(acknowledgementInterval);
        rmAssertion.setBaseRetransmissionInterval(baseRetransmissionInterval);

        rmFeature.setRMAssertion(rmAssertion);

        return rmFeature;
    }
}
