/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkiverse.cxf.client.it;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class CxfClientTestResource implements QuarkusTestResourceLifecycleManager {

    private static final int PROXY_PORT = 8080;
    private static final String SOAP_SERVICE_HOST = "calculator-service";
    private static final int SOAP_SERVICE_PORT = 8080;
    private GenericContainer<?> proxy;
    private GenericContainer<?> calculatorContainer;
    private Network network;
    private GenericContainer<?> skewedCalculatorContainer;

    @Override
    public Map<String, String> start() {

        network = Network.newNetwork();

        final String PROXY_USER = "proxyuser";
        final String PROXY_PASSWORD = UUID.randomUUID().toString();

        final String BASIC_AUTH_USER = "tester";
        final String BASIC_AUTH_PASSWORD = UUID.randomUUID().toString();

        try {
            calculatorContainer = new GenericContainer<>("quay.io/l2x6/calculator-ws:1.3")
                    .withExposedPorts(SOAP_SERVICE_PORT)
                    .withNetworkAliases(SOAP_SERVICE_HOST)
                    .withNetwork(network)
                    .withEnv("BASIC_AUTH_USER", BASIC_AUTH_USER)
                    .withEnv("BASIC_AUTH_PASSWORD", BASIC_AUTH_PASSWORD)
                    .waitingFor(Wait.forHttp("/calculator-ws/CalculatorService?wsdl"));

            calculatorContainer.start();

            skewedCalculatorContainer = new GenericContainer<>("quay.io/l2x6/calculator-ws:1.3")
                    .withEnv("ADD_TO_RESULT", "100")
                    .withExposedPorts(SOAP_SERVICE_PORT)
                    .waitingFor(Wait.forHttp("/calculator-ws/CalculatorService?wsdl"));

            skewedCalculatorContainer.start();

            proxy = new GenericContainer<>("mitmproxy/mitmproxy:10.0.0")
                    .withCommand("mitmdump", "--set", "proxyauth=" + PROXY_USER + ":" + PROXY_PASSWORD)
                    .withExposedPorts(PROXY_PORT)
                    .withNetwork(network)
                    .waitingFor(Wait.forListeningPort());
            proxy.start();

            final Map<String, String> props = new LinkedHashMap<>();
            props.put("cxf.it.calculator.baseUri",
                    "http://" + calculatorContainer.getHost() + ":" + calculatorContainer.getMappedPort(SOAP_SERVICE_PORT));
            props.put("cxf.it.skewed-calculator.baseUri",
                    "http://" + skewedCalculatorContainer.getHost() + ":"
                            + skewedCalculatorContainer.getMappedPort(SOAP_SERVICE_PORT));
            props.put("cxf.it.calculator.auth.basic.user", BASIC_AUTH_USER);
            props.put("cxf.it.calculator.auth.basic.password", BASIC_AUTH_PASSWORD);

            props.put(
                    "cxf.it.calculator.hostNameUri",
                    "http://" + SOAP_SERVICE_HOST + ":" + SOAP_SERVICE_PORT);

            props.put("cxf.it.calculator.proxy.host", proxy.getHost());
            props.put("cxf.it.calculator.proxy.port", String.valueOf(proxy.getMappedPort(PROXY_PORT)));
            props.put("cxf.it.calculator.proxy.user", PROXY_USER);
            props.put("cxf.it.calculator.proxy.password", PROXY_PASSWORD);

            return props;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (calculatorContainer != null) {
                calculatorContainer.stop();
            }
        } catch (Exception e) {
            // ignored
        }
        try {
            if (skewedCalculatorContainer != null) {
                skewedCalculatorContainer.stop();
            }
        } catch (Exception e) {
            // ignored
        }
        try {
            if (proxy != null) {
                proxy.stop();
            }
        } catch (Exception e) {
            // ignored
        }
        try {
            if (network != null) {
                network.close();
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
