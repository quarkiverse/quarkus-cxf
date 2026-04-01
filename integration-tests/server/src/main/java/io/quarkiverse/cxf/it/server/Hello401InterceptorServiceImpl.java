package io.quarkiverse.cxf.it.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.interceptor.OutInterceptors;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * The simplest Hello service implementation.
 */
@WebService(serviceName = "HelloService")
@io.quarkiverse.cxf.annotation.CXFEndpoint("/hello-401-interceptor")
@OutInterceptors(interceptors = {
        "io.quarkiverse.cxf.it.server.Hello401InterceptorServiceImpl$CustomHttpStatusOutInterceptor" })
public class Hello401InterceptorServiceImpl implements HelloService {

    @WebMethod
    @Override
    public String hello(String text) {
        return "Hello " + text;
    }

    public static class CustomHttpStatusOutInterceptor extends AbstractPhaseInterceptor<Message> {

        public CustomHttpStatusOutInterceptor() {
            super(Phase.PREPARE_SEND);
        }

        @Override
        public void handleMessage(Message message) {
            message.put(Message.RESPONSE_CODE, 401);
        }
    }
}
