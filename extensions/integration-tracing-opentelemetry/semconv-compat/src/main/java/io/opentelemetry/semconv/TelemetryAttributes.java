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
public final class TelemetryAttributes {

    /** The language of the telemetry SDK. */
    public static final AttributeKey<String> TELEMETRY_SDK_LANGUAGE = stringKey("telemetry.sdk.language");

    /**
     * The name of the telemetry SDK as defined above.
     *
     * <p>
     * Notes:
     *
     * <ul>
     * <li>The OpenTelemetry SDK MUST set the {@code telemetry.sdk.name} attribute to {@code
     *       opentelemetry}. If another SDK, like a fork or a vendor-provided implementation, is used,
     * this SDK MUST set the {@code telemetry.sdk.name} attribute to the fully-qualified class
     * or module name of this SDK's main entry point or another suitable identifier depending on
     * the language. The identifier {@code opentelemetry} is reserved and MUST NOT be used in
     * this case. All custom identifiers SHOULD be stable across different versions of an
     * implementation.
     * </ul>
     */
    public static final AttributeKey<String> TELEMETRY_SDK_NAME = stringKey("telemetry.sdk.name");

    /** The version string of the telemetry SDK. */
    public static final AttributeKey<String> TELEMETRY_SDK_VERSION = stringKey("telemetry.sdk.version");

    // Enum definitions
    /** Values for {@link #TELEMETRY_SDK_LANGUAGE}. */
    public static final class TelemetrySdkLanguageValues {
        /** cpp. */
        public static final String CPP = "cpp";

        /** dotnet. */
        public static final String DOTNET = "dotnet";

        /** erlang. */
        public static final String ERLANG = "erlang";

        /** go. */
        public static final String GO = "go";

        /** java. */
        public static final String JAVA = "java";

        /** nodejs. */
        public static final String NODEJS = "nodejs";

        /** php. */
        public static final String PHP = "php";

        /** python. */
        public static final String PYTHON = "python";

        /** ruby. */
        public static final String RUBY = "ruby";

        /** rust. */
        public static final String RUST = "rust";

        /** swift. */
        public static final String SWIFT = "swift";

        /** webjs. */
        public static final String WEBJS = "webjs";

        private TelemetrySdkLanguageValues() {
        }
    }

    private TelemetryAttributes() {
    }
}
