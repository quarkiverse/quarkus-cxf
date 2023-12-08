package io.quarkiverse.cxf.it.ws.rm.server;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.addressing.WSAContextUtils;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.cxf.ws.rm.RM11Constants;
import org.apache.cxf.ws.rm.feature.RMFeature;
import org.apache.cxf.ws.rmp.v200502.RMAssertion;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class WsReliableMessagingTest {

    private static final Logger LOG = Logger.getLogger(WsReliableMessagingTest.class);

    private static final String GREETME_ACTION = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-rm/WsrmHelloService/sayHelloRequest";
    private static final String GREETME_RESPONSE_ACTION = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-rm/WsrmHelloService/sayHelloResponse";

    @Test
    public void testTwowayMessageLoss() throws Exception {

        CXFBusFactory busFactory = new CXFBusFactory();
        Bus bus = busFactory.createBus();
        CXFBusFactory.setDefaultBus(bus);
        QName serviceName = new QName("https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-rm",
                "WsrmHelloService");
        Service service = Service.create(new URL(io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil.getServerUrl()
                + "/services/wsrm?wsdl"),
                serviceName);
        //final LoggingFeature loggingFeature = new LoggingFeature();
        //loggingFeature.setPrettyLogging(true);
        //RMStoreFeature rmFeature = new RMStoreFeature();
        RMFeature rmFeature = new RMFeature();
        RMAssertion.BaseRetransmissionInterval baseRetransmissionInterval = new RMAssertion.BaseRetransmissionInterval();
        baseRetransmissionInterval.setMilliseconds(Long.valueOf(4000));
        RMAssertion.AcknowledgementInterval acknowledgementInterval = new RMAssertion.AcknowledgementInterval();
        acknowledgementInterval.setMilliseconds(Long.valueOf(2000));
        rmFeature.setRMNamespace(RM11Constants.NAMESPACE_URI);
        RMAssertion rmAssertion = new RMAssertion();
        rmAssertion.setAcknowledgementInterval(acknowledgementInterval);
        rmAssertion.setBaseRetransmissionInterval(baseRetransmissionInterval);

        /*
         * AcksPolicyType acksPolicy = new AcksPolicyType();
         * acksPolicy.setIntraMessageThreshold(0);
         * DestinationPolicyType destinationPolicy = new DestinationPolicyType();
         * destinationPolicy.setAcksPolicy(acksPolicy);
         */

        rmFeature.setRMAssertion(rmAssertion);
        //rmFeature.setDestinationPolicy(destinationPolicy);

        WsrmHelloService proxy = service.getPort(
                WsrmHelloService.class,
                new WSAddressingFeature(),
                rmFeature);
        String decoupledEndpoint = "/wsrm/decoupled_endpoint";
        Client client = ClientProxy.getClient(proxy);
        LoggingOutInterceptor loggingOut = new LoggingOutInterceptor();
        loggingOut.setPrettyLogging(true);
        LoggingInInterceptor loggingIn = new LoggingInInterceptor();
        loggingIn.setPrettyLogging(true);

        final OutMessageRecorder outRecorder = new OutMessageRecorder();
        bus.getOutInterceptors().add(outRecorder);
        bus.getOutInterceptors().add(loggingOut);
        final InMessageRecorder inRecorder = new InMessageRecorder();
        client.getInInterceptors().add(inRecorder);
        bus.getInInterceptors().add(loggingIn);
        client.getOutInterceptors().add(new MessageLossSimulator());
        HTTPConduit hc = (HTTPConduit) (client.getConduit());
        HTTPClientPolicy cp = hc.getClient();

        cp.setDecoupledEndpoint(decoupledEndpoint);
        bus.setProperty(WSAContextUtils.DECOUPLED_ENDPOINT_BASE_PROPERTY,
                "http://localhost:8081/services");

        baseRetransmissionInterval.setMilliseconds(Long.valueOf(2000));
        ConnectionHelper.setKeepAliveConnection(proxy, true);

        LOG.info("About to send Joe");
        proxy.sayHello("Joe");
        LOG.info("Received Joe, about to send Tom");
        proxy.sayHello("Tom");
        LOG.info("Received Tom, about to send Paul");
        proxy.sayHello("Paul");
        LOG.info("Received Paul, about to send Max");
        proxy.sayHello("Max");
        LOG.info("Received Max");

        new MessageRecorder(outRecorder, inRecorder).awaitMessages(7, 5, 10000);

        final MessageFlowAssertions mf = new MessageFlowAssertions(outRecorder.getOutboundMessages(), Names.WSA_NAMESPACE_NAME,
                RM11Constants.NAMESPACE_URI, "Outbound");

        // Expected outbound:
        // CreateSequence
        // + 4 greetMe messages
        // + 2 resends

        String[] expectedActions = new String[7];
        expectedActions[0] = RM11Constants.CREATE_SEQUENCE_ACTION;
        for (int i = 1; i < expectedActions.length; i++) {
            expectedActions[i] = GREETME_ACTION;
        }
        mf.verifyActions(expectedActions);
        mf.verifyMessageNumbers(null, "1", "2", "2", "3", "4", "4");
        mf.verifyLastMessage(new boolean[7]);
        boolean[] expectedAcks = new boolean[7];
        for (int i = 2; i < expectedAcks.length; i++) {
            expectedAcks[i] = true;
        }
        mf.verifyAcknowledgements(expectedAcks);

        // Expected inbound:
        // createSequenceResponse
        // + 4 greetMeResponse actions (to original or resent)

        final MessageFlowAssertions outAssertions = new MessageFlowAssertions(inRecorder.getInboundMessages(),
                Names.WSA_NAMESPACE_NAME, RM11Constants.NAMESPACE_URI, "Inbound");

        outAssertions.verifyActions(RM11Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
                GREETME_RESPONSE_ACTION, GREETME_RESPONSE_ACTION,
                GREETME_RESPONSE_ACTION, GREETME_RESPONSE_ACTION);
        outAssertions.verifyMessageNumbers(new String[] { null, "1", "2", "3", "4" });
        outAssertions.verifyAcknowledgements(new boolean[] { false, true, true, true, true });

    }

}
