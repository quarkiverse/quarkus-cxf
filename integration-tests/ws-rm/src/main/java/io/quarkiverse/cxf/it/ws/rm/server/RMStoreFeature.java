package io.quarkiverse.cxf.it.ws.rm.server;

import java.sql.Connection;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import jakarta.enterprise.inject.spi.CDI;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.ws.rm.DestinationSequence;
import org.apache.cxf.ws.rm.RM11Constants;
import org.apache.cxf.ws.rm.SourceSequence;
import org.apache.cxf.ws.rm.feature.RMFeature;
import org.apache.cxf.ws.rm.manager.AcksPolicyType;
import org.apache.cxf.ws.rm.manager.DestinationPolicyType;
import org.apache.cxf.ws.rm.persistence.RMMessage;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.persistence.jdbc.RMTxStore;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rmp.v200502.RMAssertion;
import org.apache.cxf.ws.rmp.v200502.RMAssertion.BaseRetransmissionInterval;

/**
 * Another {@link RMFeature} which allows set {@link Connection} or {@link DataSource} to store
 * RMSequence or DestionationSequence message
 *
 * @author <a herf="mailto:ema@redhat.com">Jim Ma</a>
 *
 */
public class RMStoreFeature extends RMFeature {

    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {

        /* CDI.current() would not work during build time */
        boolean atBuildTime = io.quarkus.runtime.Application.currentApplication() == null;
        if (provider instanceof Client || atBuildTime) {
            /* As lazy as we are to bother with setting up the data source within the test class path in native mode */
            this.setStore(new RMMemoryStore());
        } else {
            final RMTxStore rmStore = new RMTxStore();
            rmStore.setDataSource(CDI.current().select(DataSource.class).get());
            rmStore.init();
            this.setStore(rmStore);
        }

        // force to use RM11 and it can only work with wsa200508 (http://www.w3.org/2005/08/addressing) which is enabled
        // with @Addressing
        this.setRMNamespace(RM11Constants.NAMESPACE_URI);
        RMAssertion assertion = new RMAssertion();

        BaseRetransmissionInterval retransMissionInveral = new BaseRetransmissionInterval();
        retransMissionInveral.setMilliseconds(4000L);
        assertion.setBaseRetransmissionInterval(retransMissionInveral);

        RMAssertion.AcknowledgementInterval acknowledgementInterval = new RMAssertion.AcknowledgementInterval();
        acknowledgementInterval.setMilliseconds(2000L);
        assertion.setAcknowledgementInterval(acknowledgementInterval);

        this.setRMAssertion(assertion);

        AcksPolicyType acksPolicy = new AcksPolicyType();
        acksPolicy.setIntraMessageThreshold(0);
        DestinationPolicyType destinationPolicy = new DestinationPolicyType();
        destinationPolicy.setAcksPolicy(acksPolicy);
        this.setDestinationPolicy(destinationPolicy);

        super.initializeProvider(provider, bus);
    }

    public static class RMMemoryStore implements RMStore {
        // during this particular test, the operations are expected to be invoked sequentially so use just HashMap
        Map<Identifier, SourceSequence> ssmap = new HashMap<>();
        Map<Identifier, DestinationSequence> dsmap = new HashMap<>();
        Map<Identifier, Collection<RMMessage>> ommap = new HashMap<>();
        Map<Identifier, Collection<RMMessage>> immap = new HashMap<>();
        Set<Identifier> ssclosed = new HashSet<>();

        @Override
        public void createSourceSequence(SourceSequence seq) {
            ssmap.put(seq.getIdentifier(), seq);
        }

        @Override
        public void createDestinationSequence(DestinationSequence seq) {
            dsmap.put(seq.getIdentifier(), seq);
        }

        @Override
        public SourceSequence getSourceSequence(Identifier seq) {
            return ssmap.get(seq);
        }

        @Override
        public DestinationSequence getDestinationSequence(Identifier seq) {
            return dsmap.get(seq);
        }

        @Override
        public void removeSourceSequence(Identifier seq) {
            ssmap.remove(seq);
        }

        @Override
        public void removeDestinationSequence(Identifier seq) {
            dsmap.remove(seq);
        }

        @Override
        public Collection<SourceSequence> getSourceSequences(String endpointIdentifier) {
            return ssmap.values();
        }

        @Override
        public Collection<DestinationSequence> getDestinationSequences(String endpointIdentifier) {
            return dsmap.values();
        }

        @Override
        public Collection<RMMessage> getMessages(Identifier sid, boolean outbound) {
            return outbound ? ommap.get(sid) : immap.get(sid);
        }

        @Override
        public void persistOutgoing(SourceSequence seq, RMMessage msg) {
            Collection<RMMessage> cm = getMessages(seq.getIdentifier(), ommap);
            if (msg != null) {
                //  update the sequence status and add the message
                cm.add(msg);
            } else {
                // update only the sequence status
                if (seq.isLastMessage()) {
                    ssclosed.add(seq.getIdentifier());
                }
            }
        }

        @Override
        public void persistIncoming(DestinationSequence seq, RMMessage msg) {
            Collection<RMMessage> cm = getMessages(seq.getIdentifier(), immap);
            if (msg != null) {
                //  update the sequence status and add the message
                cm.add(msg);
            } else {
                // update only the sequence status
            }
        }

        @Override
        public void removeMessages(Identifier sid, Collection<Long> messageNrs, boolean outbound) {
            removeMessages(sid, messageNrs, outbound ? ommap : immap);
        }

        private Collection<RMMessage> getMessages(Identifier seq, Map<Identifier, Collection<RMMessage>> map) {
            Collection<RMMessage> cm = map.get(seq);
            if (cm == null) {
                cm = new LinkedList<>();
                map.put(seq, cm);
            }
            return cm;
        }

        private void removeMessages(Identifier sid, Collection<Long> messageNrs,
                Map<Identifier, Collection<RMMessage>> map) {
            Collection<RMMessage> messages = map.get(sid);
            if (messages != null) {
                for (Iterator<RMMessage> it = messages.iterator(); it.hasNext();) {
                    RMMessage m = it.next();
                    if (messageNrs.contains(m.getMessageNumber())) {
                        it.remove();
                    }
                }
                if (messages.isEmpty()) {
                    map.remove(sid);
                }
            }
        }
    }
}
