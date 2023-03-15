/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkiverse.cxf.client.it.rtinit;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

/**
 * We declare {@link Operands} and {@link Result} as runtime initialized in {@code application.properties} to make
 * sure that we fixed <a href="https://github.com/quarkiverse/quarkus-cxf/issues/580">#580</a> properly.
 * <p>
 * The addOperands() operation is copied from
 * https://github.com/l2x6/calculator-ws/blob/1.0/src/main/java/org/jboss/as/quickstarts/wscalculator/CalculatorService.java
 * CXF should be able to use this to produce a partial client communicating with a compatible service endpoint.
 */
@WebService(targetNamespace = ClientWithRuntimeInitializedPayload.TARGET_NS, name = "CalculatorService")
public interface ClientWithRuntimeInitializedPayload {

    public static final String TARGET_NS = "http://www.jboss.org/eap/quickstarts/wscalculator/Calculator";

    @WebMethod
    public Result addOperands(Operands arg0);
}
