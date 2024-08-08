/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.semconv;

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;

// DO NOT EDIT, this is an Auto-generated file from
// buildscripts/templates/SemanticAttributes.java.j2
@SuppressWarnings("unused")
public final class ClientAttributes {

    /**
     * Client address - domain name if available without reverse DNS lookup; otherwise, IP address or
     * Unix domain socket name.
     *
     * <p>
     * Notes:
     *
     * <ul>
     * <li>When observed from the server side, and when communicating through an intermediary,
     * {@code client.address} SHOULD represent the client address behind any intermediaries, for
     * example proxies, if it's available.
     * </ul>
     */
    public static final AttributeKey<String> CLIENT_ADDRESS = stringKey("client.address");

    /**
     * Client port number.
     *
     * <p>
     * Notes:
     *
     * <ul>
     * <li>When observed from the server side, and when communicating through an intermediary,
     * {@code client.port} SHOULD represent the client port behind any intermediaries, for
     * example proxies, if it's available.
     * </ul>
     */
    public static final AttributeKey<Long> CLIENT_PORT = longKey("client.port");

    private ClientAttributes() {
    }
}
