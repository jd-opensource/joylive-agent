/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.core.util;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static com.jd.live.agent.core.Constants.EXCEPTION_MESSAGE_LABEL;
import static com.jd.live.agent.core.Constants.EXCEPTION_NAMES_LABEL;

/**
 * A utility class for working with exceptions.
 */
public class ExceptionUtils {

    public static final int HEADER_SIZE_LIMIT = 1024 * 2;

    public static final Predicate<Throwable> NONE_EXECUTION_PREDICATE = e -> !(e instanceof ExecutionException) && !(e instanceof InvocationTargetException);

    /**
     * Iterates over the exception chain starting from the given throwable and stops when the provided predicate returns false.
     *
     * @param e       the throwable to start iterating from
     * @param predicate a predicate that will be applied to each element in the exception chain
     */
    public static void iterate(Throwable e, Predicate<Throwable> predicate) {
        if (e == null || predicate == null) {
            return;
        }
        Throwable cause = e;
        while (cause != null) {
            if (predicate.test(cause)) {
                cause = cause.getCause();
            } else {
                return;
            }
        }
    }

    /**
     * Recursively retrieves the names of all exception classes in the given Throwable's cause chain.
     *
     * @param e         the Throwable from which to retrieve the exception names
     * @param predicate a predicate that will be applied to each element in the exception chain
     * @return a Set of Strings containing the names of all exception classes in the cause chain
     */
    public static Set<String> getExceptions(Throwable e, Predicate<Throwable> predicate) {
        if (e == null) {
            return null;
        }
        Set<String> names = new LinkedHashSet<>(8);
        LinkedList<Class<?>> stack = new LinkedList<>();
        iterate(e, cause -> {
            // add cause first
            if ((predicate == null || predicate.test(cause)) && names.add(cause.getClass().getName())) {
                stack.add(cause.getClass());
            }
            return true;
        });
        // add super class of cause
        Class<?> clazz;
        while (!stack.isEmpty()) {
            clazz = stack.remove();
            clazz = clazz.getSuperclass();
            while (clazz != Object.class) {
                if (names.add(clazz.getName())) {
                    clazz = clazz.getSuperclass();
                } else {
                    break;
                }
            }
        }
        return names;
    }

    /**
     * Converts a Set of Strings to a String, using the specified delimiter and truncating if the resulting String exceeds the maximum length.
     *
     * @param names     the Set of Strings to convert
     * @param delimiter the character to use as a delimiter between the Strings
     * @param maxLength the maximum length of the resulting String; if 0 or negative, no truncation occurs
     * @return a String representation of the Set, using the specified delimiter and truncation rules
     */
    public static String asString(Set<String> names, char delimiter, int maxLength) {
        if (names == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        int i = 0;
        int size = 0;
        int len;
        for (String name : names) {
            if (maxLength > 0) {
                len = name.length() + (i > 0 ? 1 : 0);
                size += len;
                if (size > maxLength) {
                    break;
                }
            }
            if (i++ > 0) {
                builder.append(delimiter);
            }
            builder.append(name);
        }
        return builder.toString();
    }

    /**
     * Describes a Throwable by extracting its exception names and message, and passing them to a BiConsumer.
     *
     * @param e         the Throwable to describe
     * @param predicate a Predicate to filter the exception names
     * @param maxLength the maximum length of the exception names and message; if 0 or negative, no truncation occurs
     * @param consumer  a BiConsumer to accept the exception names and message
     */
    public static void describe(Throwable e, Predicate<Throwable> predicate, int maxLength, BiConsumer<String, String> consumer) {
        if (consumer != null && e != null) {
            String name = asString(getExceptions(e, predicate), ',', maxLength);
            String message = e.getMessage();
            if (message != null && message.length() > maxLength) {
                message = message.substring(0, maxLength);
            }
            if (name != null || message != null) {
                consumer.accept(name, message);
            }
        }
    }

    /**
     * Generates exception-related HTTP headers from a Throwable object.
     *
     * @param e        the Throwable object to generate headers from
     * @param consumer a BiConsumer to accept the generated headers
     */
    public static void exceptionHeaders(Throwable e, BiConsumer<String, String> consumer) {
        exceptionHeaders(e, null, consumer);
    }

    /**
     * Generates exception-related HTTP headers from a Throwable object.
     *
     * @param e         the Throwable object to generate headers from
     * @param predicate a Predicate to filter the exception names
     * @param consumer  a BiConsumer to accept the generated headers
     */
    public static void exceptionHeaders(Throwable e, Predicate<Throwable> predicate, BiConsumer<String, String> consumer) {
        describe(e, predicate == null ? NONE_EXECUTION_PREDICATE : predicate, HEADER_SIZE_LIMIT, (name, message) -> {
            if (name != null && !name.isEmpty()) {
                consumer.accept(EXCEPTION_NAMES_LABEL, name);
            }
            if (message != null && !message.isEmpty()) {
                try {
                    String encodeMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.name());
                    consumer.accept(EXCEPTION_MESSAGE_LABEL, encodeMessage);
                } catch (UnsupportedEncodingException ignore) {
                }
            }
        });
    }

}
