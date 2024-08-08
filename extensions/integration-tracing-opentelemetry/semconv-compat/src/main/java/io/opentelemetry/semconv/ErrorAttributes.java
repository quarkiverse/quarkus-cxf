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
public final class ErrorAttributes {

    /**
     * Describes a class of error the operation ended with.
     *
     * <p>
     * Notes:
     *
     * <ul>
     * <li>The {@code error.type} SHOULD be predictable, and SHOULD have low cardinality.
     * <li>When {@code error.type} is set to a type (e.g., an exception type), its canonical class
     * name identifying the type within the artifact SHOULD be used.
     * <li>Instrumentations SHOULD document the list of errors they report.
     * <li>The cardinality of {@code error.type} within one instrumentation library SHOULD be low.
     * Telemetry consumers that aggregate data from multiple instrumentation libraries and
     * applications should be prepared for {@code error.type} to have high cardinality at query
     * time when no additional filters are applied.
     * <li>If the operation has completed successfully, instrumentations SHOULD NOT set {@code
     *       error.type}.
     * <li>If a specific domain defines its own set of error identifiers (such as HTTP or gRPC
     * status codes), it's RECOMMENDED to:
     * <li>Use a domain-specific attribute
     * <li>Set {@code error.type} to capture all errors, regardless of whether they are defined
     * within the domain-specific set or not.
     * </ul>
     */
    public static final AttributeKey<String> ERROR_TYPE = stringKey("error.type");

    // Enum definitions
    /** Values for {@link #ERROR_TYPE}. */
    public static final class ErrorTypeValues {
        /**
         * A fallback error value to be used when the instrumentation doesn&#39;t define a custom value.
         */
        public static final String OTHER = "_OTHER";

        private ErrorTypeValues() {
        }
    }

    private ErrorAttributes() {
    }
}
