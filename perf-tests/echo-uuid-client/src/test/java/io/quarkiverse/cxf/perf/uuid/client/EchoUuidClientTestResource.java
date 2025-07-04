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

package io.quarkiverse.cxf.perf.uuid.client;

import java.util.LinkedHashMap;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class EchoUuidClientTestResource implements QuarkusTestResourceLifecycleManager {
    private static final int SOAP_SERVICE_PORT = 8080;

    private GenericContainer<?> echoUuidService;

    @Override
    public Map<String, String> start() {

        try {
            echoUuidService = new GenericContainer<>("quay.io/l2x6/echo-uuid-ws:1.0")
                    .withExposedPorts(SOAP_SERVICE_PORT)
                    .waitingFor(Wait.forHttp("/echo-uuid-ws/soap-1.1?wsdl"));

            echoUuidService.start();

            final Map<String, String> props = new LinkedHashMap<>();
            props.put("qcxf.uuid-service.baseUri",
                    "http://" + echoUuidService.getHost() + ":" + echoUuidService.getMappedPort(SOAP_SERVICE_PORT));

            return props;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (echoUuidService != null) {
                echoUuidService.stop();
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
