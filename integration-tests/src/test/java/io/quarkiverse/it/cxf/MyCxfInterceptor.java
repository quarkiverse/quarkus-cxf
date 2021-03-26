package io.quarkiverse.it.cxf;

import org.apache.cxf.binding.soap.interceptor.SoapPreProtocolOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * MyCxfInterceptor Documentation
 *
 * <p>
 * Implemenents an CXF interceptor that throws unconditionally an illegal
 * state exception on handling a message.
 * </p>
 *
 * <p>
 * This interceptor is intented to be used for testing. Configure a
 * CXF client with this interceptor and simply test whether there is an
 * effect, i.e. expect a SOAPFaultException.
 * </p>
 *
 * @author wh81752
 */
public class MyCxfInterceptor extends AbstractPhaseInterceptor<Message> {
    public MyCxfInterceptor() {
        super(Phase.PRE_STREAM);
        addBefore(SoapPreProtocolOutInterceptor.class.getName());
    }

    public void handleMessage(Message msg) {
        throw new IllegalStateException();
    }
}
