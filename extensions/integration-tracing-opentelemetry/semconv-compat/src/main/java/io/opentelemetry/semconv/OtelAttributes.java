/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.semconv;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;

// DO NOT EDIT, this is an Auto-generated file from
// buildscripts/templates/SemanticAttributes.java.j2
@SuppressWarnings("unused")
public final class OtelAttributes {

    /** The name of the instrumentation scope - ({@code InstrumentationScope.Name} in OTLP). */
    public static final AttributeKey<String> OTEL_SCOPE_NAME = stringKey("otel.scope.name");

    /** The version of the instrumentation scope - ({@code InstrumentationScope.Version} in OTLP). */
    public static final AttributeKey<String> OTEL_SCOPE_VERSION = stringKey("otel.scope.version");

    /**
     * Name of the code, either &quot;OK&quot; or &quot;ERROR&quot;. MUST NOT be set if the status
     * code is UNSET.
     */
    public static final AttributeKey<String> OTEL_STATUS_CODE = stringKey("otel.status_code");

    /** Description of the Status if it has a value, otherwise not set. */
    public static final AttributeKey<String> OTEL_STATUS_DESCRIPTION = stringKey("otel.status_description");

    // Enum definitions
    /** Values for {@link #OTEL_STATUS_CODE}. */
    public static final class OtelStatusCodeValues {
        /**
         * The operation has been validated by an Application developer or Operator to have completed
         * successfully.
         */
        public static final String OK = "OK";

        /** The operation contains an error. */
        public static final String ERROR = "ERROR";

        private OtelStatusCodeValues() {
        }
    }

    private OtelAttributes() {
    }
}
