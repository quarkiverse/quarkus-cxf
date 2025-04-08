package io.quarkiverse.cxf.it.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.assertj.core.api.Assertions;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ServerThreadPoolStressTest {

    private static final Logger log = Logger.getLogger(ServerThreadPoolStressTest.class);
    private static final int WORKERS_COUNT = 128;
    private static final int ITERATIONS_COUNT = 4;

    @Test
    public void stress() throws Exception {
        /*
         * Unlike with the client (see RejectedExecutionException thrown in
         * io.quarkiverse.cxf.QuarkusJaxWsProxyFactoryBean.QuarkusJaxWsClientProxy.invoke(Object, Method, Object[]) and
         * io.quarkiverse.cxf.mutiny.CxfMutinyUtils.WsAsyncHandlerUni.subscribe(UniSubscriber<? super T>))
         * there is no timeout for scheduling the tasks on a worker thread on the server side.
         * So all requests should succeed (but there might occur client timeouts on slow machines)
         */

        final ExecutorService executor = Executors.newFixedThreadPool(WORKERS_COUNT);

        List<Future<int[]>> futures = new ArrayList<>(WORKERS_COUNT);
        try {

            for (int i = 0; i < WORKERS_COUNT; i++) {
                final Future<int[]> f = executor.submit(() -> {
                    final int[] codes = new int[ITERATIONS_COUNT];
                    for (int j = 0; j < ITERATIONS_COUNT; j++) {
                        final long start = System.currentTimeMillis();
                        final int result = RestAssured.given()
                                .body("""
                                        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                                          <soap:Body>
                                            <ns2:hello xmlns:ns2="http://server.it.cxf.quarkiverse.io/">
                                              <arg0>World</arg0>
                                            </ns2:hello>
                                          </soap:Body>
                                        </soap:Envelope>
                                            """)
                                .post("/soap/SlowHelloServiceImpl")
                                .then()
                                .extract().statusCode();
                        Log.infof("Slow service finished in %d ms", System.currentTimeMillis() - start);
                        codes[j] = result;
                    }
                    return codes;
                });
                futures.add(f);
            }

            final int[] expected = new int[ITERATIONS_COUNT];
            for (int j = 0; j < ITERATIONS_COUNT; j++) {
                expected[j] = 200;
            }
            // Ensure all tasks are completed
            for (Future<int[]> future : futures) {
                final int[] codes = future.get();
                Assertions.assertThat(codes).containsExactly(expected);
            }
        } finally {
            executor.shutdown();
        }
    }

}
