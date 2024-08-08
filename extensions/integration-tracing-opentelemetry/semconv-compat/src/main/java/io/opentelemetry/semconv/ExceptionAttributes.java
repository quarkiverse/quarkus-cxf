/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.semconv;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;

// DO NOT EDIT, this is an Auto-generated file from
// buildscripts/templates/SemanticAttributes.java.j2
@SuppressWarnings("unused")
public final class ExceptionAttributes {

    /**
     * SHOULD be set to true if the exception event is recorded at a point where it is known that the
     * exception is escaping the scope of the span.
     *
     * <p>
     * Notes:
     *
     * <ul>
     * <li>An exception is considered to have escaped (or left) the scope of a span, if that span is
     * ended while the exception is still logically &quot;in flight&quot;. This may be actually
     * &quot;in flight&quot; in some languages (e.g. if the exception is passed to a Context
     * manager's {@code __exit__} method in Python) but will usually be caught at the point of
     * recording the exception in most languages.
     * <li>It is usually not possible to determine at the point where an exception is thrown whether
     * it will escape the scope of a span. However, it is trivial to know that an exception will
     * escape, if one checks for an active exception just before ending the span, as done in the
     * <a
     * href="https://opentelemetry.io/docs/specs/semconv/exceptions/exceptions-spans/#recording-an-exception">example
     * for recording span exceptions</a>.
     * <li>It follows that an exception may still escape the scope of the span even if the {@code
     *       exception.escaped} attribute was not set or set to false, since the event might have been
     * recorded at a time where it was not clear whether the exception will escape.
     * </ul>
     */
    public static final AttributeKey<Boolean> EXCEPTION_ESCAPED = booleanKey("exception.escaped");

    /** The exception message. */
    public static final AttributeKey<String> EXCEPTION_MESSAGE = stringKey("exception.message");

    /**
     * A stacktrace as a string in the natural representation for the language runtime. The
     * representation is to be determined and documented by each language SIG.
     */
    public static final AttributeKey<String> EXCEPTION_STACKTRACE = stringKey("exception.stacktrace");

    /**
     * The type of the exception (its fully-qualified class name, if applicable). The dynamic type of
     * the exception should be preferred over the static type in languages that support it.
     */
    public static final AttributeKey<String> EXCEPTION_TYPE = stringKey("exception.type");

    private ExceptionAttributes() {
    }
}
