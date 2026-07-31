package io.quarkiverse.cxf.deployment.test.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class SleepyServer {
    private static final String soapResponseStart = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:helloResponse xmlns:ns2=\"http://test.deployment.cxf.quarkiverse.io/\"><return>Hello ";
    private static final String soapResponseEnd = "</return></ns2:helloResponse></soap:Body></soap:Envelope>";
    private static final Pattern REQUEST_PATTERN = Pattern.compile("<arg0>([^<]+)</arg0>");
    private final Vertx vertx;
    private final int actualPort;

    public SleepyServer(long delay) {
        /*
         * We want the server to process all requests sequentially, therefore we setWorkerPoolSize(1) and use a blockingHandler
         */
        vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(1));

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/").blockingHandler(context -> {
            HttpServerResponse resp = context.response()
                    .setChunked(true)
                    .putHeader("Content-Type", "text/xml; charset=utf-8")
                    .setStatusCode(200);
            resp.write(soapResponseStart);
            Matcher m = REQUEST_PATTERN.matcher(context.body().asString());
            String person = m.find() ? m.group(1) : null;
            if (!"Speedy".equals(person)) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    context.response().setStatusCode(500).end();
                    return;
                }
            }
            resp.end(person + soapResponseEnd);
        });

        final HttpServer server = vertx.createHttpServer(new HttpServerOptions())
                .requestHandler(router)
                .listen(-2)
                .toCompletionStage()
                .toCompletableFuture()
                .join();
        this.actualPort = server.actualPort();
    }

    public int actualPort() {
        return actualPort;
    }

    public void close() {
        vertx.close().toCompletionStage().toCompletableFuture().join();
    }
}
