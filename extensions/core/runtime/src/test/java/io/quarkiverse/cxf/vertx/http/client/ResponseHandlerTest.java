package io.quarkiverse.cxf.vertx.http.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.cxf.endpoint.ClientCallback;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.Cookies;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit.ResponseEvent;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit.ResponseHandler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;

class ResponseHandlerTest {
    private static final URI URL = URI.create("http://localhost/services/wsrm");

    @ParameterizedTest
    @CsvSource({ "before,false", "after,false", "concurrent,false", "before,true", "after,true", "concurrent,true" })
    void decoupledReplySurvivesEmptyAcknowledgement(String order, boolean propagate) throws Exception {
        Fixture fixture = new Fixture(true, false, propagate);
        Message reply = new MessageImpl();
        reply.setContent(List.class, List.of("Hello Paul!"));

        switch (order) {
            case "before":
                fixture.completeReply(reply);
                fixture.respond(202, "");
                break;
            case "after":
                fixture.respond(202, "");
                assertThat(fixture.exchange.getInMessage()).isNull();
                assertThat(fixture.exchange.get(ClientImpl.FINISHED)).isNull();
                fixture.completeReply(reply);
                break;
            case "concurrent":
                ExecutorService executor = Executors.newFixedThreadPool(2);
                try {
                    CountDownLatch start = new CountDownLatch(1);
                    var response = executor.submit(() -> {
                        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                        fixture.respond(202, "");
                        return null;
                    });
                    var delivery = executor.submit(() -> {
                        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                        fixture.completeReply(reply);
                        return null;
                    });
                    start.countDown();
                    response.get(5, TimeUnit.SECONDS);
                    delivery.get(5, TimeUnit.SECONDS);
                } finally {
                    executor.shutdownNow();
                }
                break;
            default:
                throw new IllegalArgumentException(order);
        }

        assertThat(fixture.exchange.getInMessage()).isSameAs(reply);
        List<?> contents = fixture.exchange.getInMessage().getContent(List.class);
        assertThat(contents).isEqualTo(List.of("Hello Paul!"));
        assertThat(fixture.exchange.get(ClientImpl.FINISHED)).isEqualTo(true);
        assertThat(fixture.exchange.get("IN_CHAIN_COMPLETE")).isNull();
        assertThat(fixture.exchange.get(ClientCallback.class)).isSameAs(fixture.callback);
        assertThat(fixture.callback.isDone()).isFalse();
        assertThat(fixture.observed).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "200,false", "202,false", "200,true", "202,true" })
    void oneWayAcknowledgementCompletesCallback(int status, boolean propagate) throws Exception {
        Fixture fixture = new Fixture(false, true, propagate);
        fixture.respond(status, "");

        assertThat(fixture.exchange.getInMessage().get(Message.RESPONSE_CODE)).isEqualTo(status);
        assertThat(fixture.exchange.get("IN_CHAIN_COMPLETE")).isEqualTo(true);
        assertThat(fixture.exchange.get(ClientCallback.class)).isNull();
        assertThat(fixture.callback.isDone()).isTrue();
        assertThat(fixture.observed).hasSize(propagate ? 1 : 0);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void nonDecoupled202StillPropagatesWhenRequested(boolean propagate) throws Exception {
        Fixture fixture = new Fixture(false, false, propagate);
        fixture.respond(202, "");

        assertThat(fixture.exchange.getInMessage().get(Message.RESPONSE_CODE)).isEqualTo(202);
        assertThat(fixture.exchange.get("IN_CHAIN_COMPLETE")).isEqualTo(true);
        assertThat(fixture.callback.isDone()).isTrue();
        assertThat(fixture.observed).hasSize(propagate ? 1 : 0);
    }

    @ParameterizedTest
    @ValueSource(ints = { 200, 202, 500 })
    void decoupledNonEmptyResponseStillReachesObserver(int status) throws Exception {
        Fixture fixture = new Fixture(true, false, false);
        fixture.out.put(Message.PROCESS_ONEWAY_RESPONSE, true);
        fixture.respond(status, "SOAP response or fault");

        assertThat(fixture.observed).hasSize(1);
        assertThat(fixture.observed.get(0).getContent(InputStream.class).readAllBytes())
                .isEqualTo("SOAP response or fault".getBytes(StandardCharsets.UTF_8));
        assertThat(fixture.callback.isDone()).isFalse();
    }

    private static class Fixture {
        final Exchange exchange = new ExchangeImpl();
        final Message out = new MessageImpl();
        final ClientCallback callback = new ClientCallback();
        final List<Message> observed = new ArrayList<>();

        Fixture(boolean decoupled, boolean oneWay, boolean propagate) {
            EndpointInfo info = new EndpointInfo();
            info.setBinding(new BindingInfo(null, "http://schemas.xmlsoap.org/wsdl/soap/"));
            if (decoupled) {
                info.setProperty(MAPAggregator.DECOUPLED_DESTINATION, new Object());
            }
            Endpoint endpoint = stub(Endpoint.class, (proxy, method, args) -> switch (method.getName()) {
                case "getEndpointInfo" -> info;
                case "size" -> 0;
                case "entrySet" -> Set.of();
                default -> throw new UnsupportedOperationException(method.getName());
            });
            exchange.put(Endpoint.class, endpoint);
            exchange.put(ClientCallback.class, callback);
            exchange.setOneWay(oneWay);
            exchange.setOutMessage(out);
            out.put(Message.PROPAGATE_202_RESPONSE_ONEWAY_OR_PARTIAL, propagate);
        }

        void completeReply(Message reply) {
            // Publish the decoded response as ClientImpl.onMessage() does when its inbound chain completes.
            synchronized (exchange) {
                exchange.setInMessage(reply);
                exchange.put(ClientImpl.FINISHED, true);
                exchange.notifyAll();
            }
        }

        void respond(int status, String body) throws Exception {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            MultiMap headers = MultiMap.caseInsensitiveMultiMap()
                    .add("Content-Length", Integer.toString(bytes.length))
                    .add("Content-Type", "text/xml; charset=UTF-8");
            HttpClientRequest request = stub(HttpClientRequest.class, (proxy, method, args) -> switch (method.getName()) {
                case "absoluteURI" -> URL.toString();
                default -> throw new UnsupportedOperationException(method.getName());
            });
            HttpClientResponse response = stub(HttpClientResponse.class, (proxy, method, args) -> switch (method.getName()) {
                case "request" -> request;
                case "statusCode" -> status;
                case "statusMessage" -> "Test response";
                case "headers" -> headers;
                default -> throw new UnsupportedOperationException(method.getName());
            });
            new ResponseHandler(URL, out, new Cookies(), message -> {
                observed.add(message);
                // An incoming observer such as ClientImpl may also replace the exchange's response.
                message.getExchange().setInMessage(message);
            }).handle(new ResponseEvent(response, new ByteArrayInputStream(bytes)));
        }
    }

    private static <T> T stub(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler));
    }
}
