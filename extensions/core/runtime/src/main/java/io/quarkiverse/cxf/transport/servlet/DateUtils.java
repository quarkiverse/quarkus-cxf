/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.quarkiverse.cxf.transport.servlet;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility for parsing and generating dates
 *
 * Copied from
 * https://github.com/quarkusio/quarkus-http/blob/34d4ca77197cfb8d99bef09f1b12081dd2cef8a8/core/src/main/java/io/undertow/util/DateUtils.java
 *
 * @author Stuart Douglas
 */
public class DateUtils {

    private static final Locale LOCALE_US = Locale.US;

    private static final TimeZone GMT_ZONE = TimeZone.getTimeZone("GMT");

    private static final String RFC1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";

    private static final AtomicReference<String> cachedDateString = new AtomicReference<>();

    /**
     * Thread local cache of this date format. This is technically a small memory leak, however
     * in practice it is fine, as it will only be used by server threads.
     * <p/>
     * This is the most common date format, which is why we cache it.
     */
    private static final ThreadLocal<SimpleDateFormat> RFC1123_PATTERN_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat df = new SimpleDateFormat(RFC1123_PATTERN, LOCALE_US);
            return df;
        }
    };

    /**
     * Invalidates the current date
     */
    private static final Runnable INVALIDATE_TASK = new Runnable() {
        @Override
        public void run() {
            cachedDateString.set(null);
        }
    };

    private static final String RFC1036_PATTERN = "EEEEEEEEE, dd-MMM-yy HH:mm:ss z";

    private static final String ASCITIME_PATTERN = "EEE MMM d HH:mm:ss yyyyy";

    private static final String OLD_COOKIE_PATTERN = "EEE, dd-MMM-yyyy HH:mm:ss z";

    private static final String COMMON_LOG_PATTERN = "[dd/MMM/yyyy:HH:mm:ss Z]";

    private static final ThreadLocal<SimpleDateFormat> COMMON_LOG_PATTERN_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat df = new SimpleDateFormat(COMMON_LOG_PATTERN, LOCALE_US);
            return df;
        }
    };

    private static final ThreadLocal<SimpleDateFormat> OLD_COOKIE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat df = new SimpleDateFormat(OLD_COOKIE_PATTERN, LOCALE_US);
            df.setTimeZone(GMT_ZONE);
            return df;
        }
    };

    /**
     * Converts a date to a format suitable for use in a HTTP request
     *
     * @param date The date
     * @return The RFC-1123 formatted date
     */
    public static String toDateString(final Date date) {
        SimpleDateFormat df = RFC1123_PATTERN_FORMAT.get();
        //we always need to set the time zone
        //because date format is stupid, and calling parse() can mutate the timezone
        //see UNDERTOW-458
        df.setTimeZone(GMT_ZONE);
        return df.format(date);
    }

    public static String toOldCookieDateString(final Date date) {
        return OLD_COOKIE_FORMAT.get().format(date);
    }

    public static String toCommonLogFormat(final Date date) {
        return COMMON_LOG_PATTERN_FORMAT.get().format(date);
    }

    /**
     * Attempts to pass a HTTP date.
     *
     * @param date The date to parse
     * @return The parsed date, or null if parsing failed
     */
    public static Date parseDate(final String date) {

        /*
         * IE9 sends a superflous lenght parameter after date in the
         * If-Modified-Since header, which needs to be stripped before
         * parsing.
         *
         */

        final int semicolonIndex = date.indexOf(';');
        final String trimmedDate = semicolonIndex >= 0 ? date.substring(0, semicolonIndex) : date;

        ParsePosition pp = new ParsePosition(0);
        SimpleDateFormat dateFormat = RFC1123_PATTERN_FORMAT.get();
        dateFormat.setTimeZone(GMT_ZONE);
        Date val = dateFormat.parse(trimmedDate, pp);
        if (val != null && pp.getIndex() == trimmedDate.length()) {
            return val;
        }

        pp = new ParsePosition(0);
        dateFormat = new SimpleDateFormat(RFC1036_PATTERN, LOCALE_US);
        dateFormat.setTimeZone(GMT_ZONE);
        val = dateFormat.parse(trimmedDate, pp);
        if (val != null && pp.getIndex() == trimmedDate.length()) {
            return val;
        }

        pp = new ParsePosition(0);
        dateFormat = new SimpleDateFormat(ASCITIME_PATTERN, LOCALE_US);
        dateFormat.setTimeZone(GMT_ZONE);
        val = dateFormat.parse(trimmedDate, pp);
        if (val != null && pp.getIndex() == trimmedDate.length()) {
            return val;
        }

        pp = new ParsePosition(0);
        dateFormat = new SimpleDateFormat(OLD_COOKIE_PATTERN, LOCALE_US);
        dateFormat.setTimeZone(GMT_ZONE);
        val = dateFormat.parse(trimmedDate, pp);
        if (val != null && pp.getIndex() == trimmedDate.length()) {
            return val;
        }

        return null;
    }

    private DateUtils() {

    }

}
