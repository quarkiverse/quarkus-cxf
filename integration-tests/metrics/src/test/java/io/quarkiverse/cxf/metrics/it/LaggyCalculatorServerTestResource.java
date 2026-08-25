package io.quarkiverse.cxf.metrics.it;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class LaggyCalculatorServerTestResource implements QuarkusTestResourceLifecycleManager {

    static final long DELAY = 1000L;
    static final int CLIENT_POOL_SIZE = 5;
    static final int ITERATION_COUNT = 2;

    private static final String SOAP_RESPONSE_START = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:addResponse xmlns:ns2=\"http://www.jboss.org/eap/quickstarts/wscalculator/Calculator\"><return>";
    private static final String SOAP_RESPONSE_END = "</return></ns2:addResponse></soap:Body></soap:Envelope>";
    private static final Pattern ARG_PATTERN = Pattern.compile("<arg0>([^<]+)</arg0>.*<arg1>([^<]+)</arg1>");

    private Vertx vertx;

    @Override
    public Map<String, String> start() {
        vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(CLIENT_POOL_SIZE * 2));

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/")
                .handler(ctx -> {
                    AtomicBoolean clientGone = new AtomicBoolean(false);
                    ctx.request().connection().closeHandler(v -> clientGone.set(true));
                    ctx.put("clientGone", clientGone);
                    ctx.next();
                })
                .blockingHandler(context -> {
                    String body = context.body().asString();
                    Matcher m = ARG_PATTERN.matcher(body);
                    int arg0 = -1;
                    int arg1 = -1;
                    if (m.find()) {
                        arg0 = Integer.parseInt(m.group(1));
                        arg1 = Integer.parseInt(m.group(2));
                    }
                    AtomicBoolean clientGone = context.get("clientGone");
                    if (arg0 != 0) {
                        long deadline = System.currentTimeMillis() + DELAY;
                        while (!clientGone.get() && System.currentTimeMillis() < deadline) {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                context.response().setStatusCode(500).end();
                                return;
                            }
                        }
                    }
                    HttpServerResponse resp = context.response()
                            .setChunked(true)
                            .putHeader("Content-Type", "text/xml; charset=utf-8")
                            .setStatusCode(200);
                    resp.write(SOAP_RESPONSE_START);
                    resp.end(String.valueOf(arg0 + arg1) + SOAP_RESPONSE_END);
                });

        HttpServer server = vertx.createHttpServer(new HttpServerOptions())
                .requestHandler(router)
                .listen(0)
                .toCompletionStage()
                .toCompletableFuture()
                .join();

        return Map.of(
                "qcxf.laggy-calculator.url", "http://localhost:" + server.actualPort() + "/",
                "qcxf.laggy.pool-size", String.valueOf(CLIENT_POOL_SIZE),
                "qcxf.laggy.iterations", String.valueOf(ITERATION_COUNT));
    }

    @Override
    public void stop() {
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().join();
        }
    }
}
