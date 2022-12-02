package io.quarkiverse.cxf.it.ws.rm.server;

import java.util.Collection;
import java.util.Iterator;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.rm.RMDeliveryInterceptor;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.SourceSequence;

/**
 * Interceptor to check if the RMStore is enabled and stores data
 *
 * @author <a herf="mailto:ema@redhat.com">Jim Ma</a>
 *
 */
public class RMStoreCheckInterceptor extends AbstractPhaseInterceptor<Message> {

    public static volatile int seqSize;
    private String endpointIdentifier = "{https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-rm}WsrmHelloService.{https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-rm}WsrmHelloServicePort@cxf";

    public RMStoreCheckInterceptor() {
        super(Phase.POST_INVOKE);
        this.addBefore(RMDeliveryInterceptor.class.getName());
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        RMManager rmManager = message.getExchange().getBus().getExtension(RMManager.class);
        Collection<SourceSequence> seqs = rmManager.getStore().getSourceSequences(endpointIdentifier);
        if (seqs != null) {
            seqSize = seqs.size();

            for (Iterator<SourceSequence> iterator = seqs.iterator(); iterator.hasNext();) {
                SourceSequence sourceSequence = iterator.next();
                System.out.println("===== sourceSequence " + sourceSequence.getEndpointIdentifier() + " "
                        + sourceSequence.getIdentifier() + ": " + sourceSequence.getCurrentMessageNr());
            }
        }
    }

}
