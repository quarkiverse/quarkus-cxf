package io.quarkiverse.cxf.it.ws.rm.server;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.apache.cxf.ws.rm.RM11Constants;
import org.apache.cxf.ws.rm.feature.RMFeature;
import org.apache.cxf.ws.rmp.v200502.RMAssertion;

public class BeanProducers {

    @Produces
    @ApplicationScoped
    @Named
    RMFeature rmFeature() {
        org.apache.cxf.ws.rm.feature.RMFeature rmFeature = new org.apache.cxf.ws.rm.feature.RMFeature();
        rmFeature.setRMNamespace(RM11Constants.NAMESPACE_URI);
        RMAssertion.BaseRetransmissionInterval baseRetransmissionInterval = new RMAssertion.BaseRetransmissionInterval();
        baseRetransmissionInterval.setMilliseconds(6000L);
        RMAssertion.AcknowledgementInterval acknowledgementInterval = new RMAssertion.AcknowledgementInterval();
        acknowledgementInterval.setMilliseconds(2000L);

        RMAssertion rmAssertion = new RMAssertion();
        rmAssertion.setAcknowledgementInterval(acknowledgementInterval);
        rmAssertion.setBaseRetransmissionInterval(baseRetransmissionInterval);

        rmFeature.setRMAssertion(rmAssertion);

        return rmFeature;
    }
}
