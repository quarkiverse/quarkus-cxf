package io.quarkiverse.cxf.client.it;

import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.binding.soap.interceptor.SoapPreProtocolOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * Implemenents an CXF interceptor that throws unconditionally an illegal
 * state exception on handling a message.
 *
 * <p>
 * This interceptor is intented to be used for testing. Configure a
 * CXF client with this interceptor and simply test whether there is an
 * effect, i.e. expect a {@link SOAPFaultException} on sending a message.
 * </p>
 */
public class SOAPExceptionInterceptor extends AbstractPhaseInterceptor<Message> {
    public SOAPExceptionInterceptor() {
        super(Phase.PRE_STREAM);
        addBefore(SoapPreProtocolOutInterceptor.class.getName());
    }

    public void handleMessage(Message msg) {
        throw new IllegalStateException("No luck at this time, Luke!");
    }
}
