package io.quarkiverse.cxf.it.ws.rm.client;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.ws.addressing.WSAContextUtils;

import io.quarkus.runtime.StartupEvent;

public class Init {

    @Inject
    @Named
    OutMessageRecorder outMessageRecorder;

    void onStart(@Observes StartupEvent ev) {
        final Bus bus = BusFactory.getDefaultBus();

        LoggingOutInterceptor loggingOut = new LoggingOutInterceptor();
        loggingOut.setPrettyLogging(true);
        LoggingInInterceptor loggingIn = new LoggingInInterceptor();
        loggingIn.setPrettyLogging(true);
        bus.getOutInterceptors().add(loggingOut);
        bus.getInInterceptors().add(loggingIn);

        bus.getOutInterceptors().add(outMessageRecorder);
        bus.setProperty(WSAContextUtils.DECOUPLED_ENDPOINT_BASE_PROPERTY, "http://localhost:8081/services");

    }
}
