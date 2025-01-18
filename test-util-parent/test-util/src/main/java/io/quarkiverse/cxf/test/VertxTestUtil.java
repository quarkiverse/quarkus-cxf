package io.quarkiverse.cxf.test;

import org.assertj.core.api.Assertions;

import io.vertx.core.AsyncResult;

public class VertxTestUtil {

    private VertxTestUtil() {
    }

    public static <T> T assertSuccess(AsyncResult<T> result) {
        Assertions.assertThat(result).satisfiesAnyOf(
                i -> Assertions.assertThat(i.succeeded()).isTrue(),
                i -> {
                    throw i.cause();
                });
        return result.result();
    }

}
