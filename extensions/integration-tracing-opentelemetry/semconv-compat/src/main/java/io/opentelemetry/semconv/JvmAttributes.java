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
public final class JvmAttributes {

    /**
     * Name of the garbage collector action.
     *
     * <p>
     * Notes:
     *
     * <ul>
     * <li>Garbage collector action is generally obtained via <a
     * href=
     * "https://docs.oracle.com/en/java/javase/11/docs/api/jdk.management/com/sun/management/GarbageCollectionNotificationInfo.html#getGcAction()">GarbageCollectionNotificationInfo#getGcAction()</a>.
     * </ul>
     */
    public static final AttributeKey<String> JVM_GC_ACTION = stringKey("jvm.gc.action");

    /**
     * Name of the garbage collector.
     *
     * <p>
     * Notes:
     *
     * <ul>
     * <li>Garbage collector name is generally obtained via <a
     * href=
     * "https://docs.oracle.com/en/java/javase/11/docs/api/jdk.management/com/sun/management/GarbageCollectionNotificationInfo.html#getGcName()">GarbageCollectionNotificationInfo#getGcName()</a>.
     * </ul>
     */
    public static final AttributeKey<String> JVM_GC_NAME = stringKey("jvm.gc.name");

    /**
     * Name of the memory pool.
     *
     * <p>
     * Notes:
     *
     * <ul>
     * <li>Pool names are generally obtained via <a
     * href=
     * "https://docs.oracle.com/en/java/javase/11/docs/api/java.management/java/lang/management/MemoryPoolMXBean.html#getName()">MemoryPoolMXBean#getName()</a>.
     * </ul>
     */
    public static final AttributeKey<String> JVM_MEMORY_POOL_NAME = stringKey("jvm.memory.pool.name");

    /** The type of memory. */
    public static final AttributeKey<String> JVM_MEMORY_TYPE = stringKey("jvm.memory.type");

    /** Whether the thread is daemon or not. */
    public static final AttributeKey<Boolean> JVM_THREAD_DAEMON = booleanKey("jvm.thread.daemon");

    /** State of the thread. */
    public static final AttributeKey<String> JVM_THREAD_STATE = stringKey("jvm.thread.state");

    // Enum definitions
    /** Values for {@link #JVM_MEMORY_TYPE}. */
    public static final class JvmMemoryTypeValues {
        /** Heap memory. */
        public static final String HEAP = "heap";

        /** Non-heap memory. */
        public static final String NON_HEAP = "non_heap";

        private JvmMemoryTypeValues() {
        }
    }

    /** Values for {@link #JVM_THREAD_STATE}. */
    public static final class JvmThreadStateValues {
        /** A thread that has not yet started is in this state. */
        public static final String NEW = "new";

        /** A thread executing in the Java virtual machine is in this state. */
        public static final String RUNNABLE = "runnable";

        /** A thread that is blocked waiting for a monitor lock is in this state. */
        public static final String BLOCKED = "blocked";

        /**
         * A thread that is waiting indefinitely for another thread to perform a particular action is in
         * this state.
         */
        public static final String WAITING = "waiting";

        /**
         * A thread that is waiting for another thread to perform an action for up to a specified
         * waiting time is in this state.
         */
        public static final String TIMED_WAITING = "timed_waiting";

        /** A thread that has exited is in this state. */
        public static final String TERMINATED = "terminated";

        private JvmThreadStateValues() {
        }
    }

    private JvmAttributes() {
    }
}
