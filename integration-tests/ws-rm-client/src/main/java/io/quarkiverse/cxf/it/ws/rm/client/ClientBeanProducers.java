package io.quarkiverse.cxf.it.ws.rm.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.apache.cxf.ws.rm.RM11Constants;
import org.apache.cxf.ws.rm.feature.RMFeature;
import org.apache.cxf.ws.rmp.v200502.RMAssertion;

public class ClientBeanProducers {

    @Produces
    @ApplicationScoped
    @Named
    MessageLossSimulator messageLossSimulator() {
        return new MessageLossSimulator();
    }

    @Produces
    @ApplicationScoped
    @Named
    InMessageRecorder inMessageRecorder() {
        return new InMessageRecorder();
    }

    @Produces
    @ApplicationScoped
    @Named
    OutMessageRecorder outMessageRecorder() {
        return new OutMessageRecorder();
    }

    @Produces
    @ApplicationScoped
    @Named
    RMFeature rmFeatureClient() {
        RMFeature rmFeature = new RMFeature();
        rmFeature.setRMNamespace(RM11Constants.NAMESPACE_URI);
        RMAssertion.BaseRetransmissionInterval baseRetransmissionInterval = new RMAssertion.BaseRetransmissionInterval();
        baseRetransmissionInterval.setMilliseconds(1000L);
        RMAssertion.AcknowledgementInterval acknowledgementInterval = new RMAssertion.AcknowledgementInterval();
        acknowledgementInterval.setMilliseconds(500L);

        RMAssertion rmAssertion = new RMAssertion();
        rmAssertion.setAcknowledgementInterval(acknowledgementInterval);
        rmAssertion.setBaseRetransmissionInterval(baseRetransmissionInterval);

        rmFeature.setRMAssertion(rmAssertion);

        return rmFeature;
    }
}
