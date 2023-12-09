package io.quarkiverse.cxf.it.ws.rm.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

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
}
