package io.quarkiverse.cxf.it.ws.rm.server;

import jakarta.enterprise.event.Observes;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;

import io.quarkus.runtime.StartupEvent;

public class Init {

    void init(@Observes StartupEvent start) {
        Bus bus = BusFactory.getDefaultBus();
        LoggingOutInterceptor loggingOut = new LoggingOutInterceptor();
        loggingOut.setPrettyLogging(true);
        LoggingInInterceptor loggingIn = new LoggingInInterceptor();
        loggingIn.setPrettyLogging(true);
        bus.getOutInterceptors().add(loggingOut);
        bus.getInInterceptors().add(loggingIn);
    }
}
