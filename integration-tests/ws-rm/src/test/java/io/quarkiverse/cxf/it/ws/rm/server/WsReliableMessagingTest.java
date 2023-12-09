package io.quarkiverse.cxf.it.ws.rm.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.rm.RM11Constants;
import org.awaitility.Awaitility;
import org.hamcrest.CoreMatchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;

@QuarkusTest
public class WsReliableMessagingTest {

    private static final Logger LOG = Logger.getLogger(WsReliableMessagingTest.class);

    private static final String GREETME_ACTION = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-rm/WsrmHelloService/helloRequest";
    private static final String GREETME_RESPONSE_ACTION = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-rm/WsrmHelloService/helloResponse";

    @Test
    public void testTwowayMessageLoss() throws Exception {

        LOG.info("About to send Joe");
        hello("Joe");
        LOG.info("Received Joe, about to send Tom");
        hello("Tom");
        LOG.info("Received Tom, about to send Paul");
        hello("Paul");
        LOG.info("Received Paul, about to send Max");
        hello("Max");
        LOG.info("Received Max");

        final MessageFlowAssertions outAssertions = new MessageFlowAssertions(
                awaitMessages("out", 7),
                Names.WSA_NAMESPACE_NAME,
                RM11Constants.NAMESPACE_URI,
                "Outbound");

        // Expected outbound:
        // CreateSequence
        // + 4 greetMe messages
        // + 2 resends

        String[] expectedActions = new String[7];
        expectedActions[0] = RM11Constants.CREATE_SEQUENCE_ACTION;
        for (int i = 1; i < expectedActions.length; i++) {
            expectedActions[i] = GREETME_ACTION;
        }
        outAssertions.verifyActions(expectedActions);
        outAssertions.verifyMessageNumbers(null, "1", "2", "2", "3", "4", "4");
        outAssertions.verifyLastMessage(new boolean[7]);
        boolean[] expectedAcks = new boolean[7];
        for (int i = 2; i < expectedAcks.length; i++) {
            expectedAcks[i] = true;
        }
        outAssertions.verifyAcknowledgements(expectedAcks);

        // Expected inbound:
        // createSequenceResponse
        // + 4 greetMeResponse actions (to original or resent)

        final MessageFlowAssertions inAssertions = new MessageFlowAssertions(
                awaitMessages("in", 5),
                Names.WSA_NAMESPACE_NAME,
                RM11Constants.NAMESPACE_URI,
                "Inbound");

        inAssertions.verifyActions(RM11Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
                GREETME_RESPONSE_ACTION, GREETME_RESPONSE_ACTION,
                GREETME_RESPONSE_ACTION, GREETME_RESPONSE_ACTION);
        inAssertions.verifyMessageNumbers(new String[] { null, "1", "2", "3", "4" });
        inAssertions.verifyAcknowledgements(new boolean[] { false, true, true, true, true });

    }

    private List<String> awaitMessages(String direction, int count) {
        final List<String> result = new ArrayList<>();
        Awaitility.waitAtMost(30, TimeUnit.SECONDS)
                .until(
                        () -> {
                            ValidatableResponse response = RestAssured.given()
                                    .get("/wsrm-rest/messages/" + direction)
                                    .then();
                            int statusCode = response.extract().statusCode();
                            switch (statusCode) {
                                case 204:
                                    return false;
                                case 200:
                                    Stream.of(response.extract().body().asString().split("\\Q|||\\E"))
                                            .peek(m -> System.out.println("====(" + direction + ") " + m + "="))
                                            .forEach(result::add);
                                    return result.size() >= count;
                                default:
                                    throw new IllegalStateException(
                                            "/wsrm-rest/messages/" + direction + " returned " + statusCode);
                            }
                        });

        return result;
    }

    static void hello(String name) {
        RestAssured.given()
                .body(name)
                .post("/wsrm-rest/hello")
                .then()
                .statusCode(200)
                .body(CoreMatchers.startsWith("WS-ReliableMessaging Hello " + name + "! counter: "));
    }

}
