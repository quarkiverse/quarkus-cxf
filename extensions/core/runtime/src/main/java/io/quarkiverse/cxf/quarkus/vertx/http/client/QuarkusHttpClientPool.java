package io.quarkiverse.cxf.quarkus.vertx.http.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.cxf.vertx.http.client.HttpClientPool;
import io.vertx.core.Vertx;

@ApplicationScoped
public class QuarkusHttpClientPool extends HttpClientPool {

    QuarkusHttpClientPool() {
        super(null);
    }

    @Inject
    public QuarkusHttpClientPool(Vertx vertx) {
        super(vertx);
    }
}
