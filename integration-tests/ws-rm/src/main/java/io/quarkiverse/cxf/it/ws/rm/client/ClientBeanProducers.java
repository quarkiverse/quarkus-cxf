package io.quarkiverse.cxf.it.ws.rm.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.apache.cxf.ws.rm.feature.RMFeature;

import io.quarkiverse.cxf.it.ws.rm.server.ServiceBeanProducers;

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
        return ServiceBeanProducers.rmFeature(4000, 2000);
    }
}
