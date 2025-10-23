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
package com.jd.live.agent.governance.util;

import com.jd.live.agent.core.util.ExceptionUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.jd.live.agent.core.Constants.LABEL_EXCEPTION_MESSAGE;
import static com.jd.live.agent.core.Constants.LABEL_EXCEPTION_NAMES;
import static com.jd.live.agent.core.util.ExceptionUtils.getExceptions;

/**
 * A utility class for working with response.
 */
public class ResponseUtils {

    public static final int HEADER_SIZE_LIMIT = 1024 * 2;

    /**
     * Converts a Set of Strings to a String, using the specified delimiter and truncating if the resulting String exceeds the maximum length.
     *
     * @param names the Set of Strings to convert
     * @return a String representation of the Set, using the specified delimiter and truncation rules
     */
    private static String join(Set<String> names) {
        if (names == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(512);
        int i = 0;
        int size = 0;
        int len;
        for (String name : names) {
            len = name.length() + (i > 0 ? 1 : 0);
            size += len;
            if (size > HEADER_SIZE_LIMIT) {
                break;
            }
            if (i++ > 0) {
                builder.append(',');
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
     * @param consumer  a BiConsumer to accept the exception names and message
     */
    private static void describe(Throwable e, Predicate<Throwable> predicate, BiConsumer<String, String> consumer) {
        describe(e, null, predicate, consumer);
    }

    /**
     * Processes a Throwable, extracting exception names and message for consumer.
     *
     * @param e           Throwable to process (nullable)
     * @param messageFunc generates message from Throwable (falls back to getMessage() if null)
     * @param predicate   filters which exceptions to include (nullable)
     * @param consumer    receives formatted names and message (truncated if exceeding limit)
     */
    private static void describe(Throwable e,
                                 Function<Throwable, String> messageFunc,
                                 Predicate<Throwable> predicate,
                                 BiConsumer<String, String> consumer) {
        if (consumer != null && e != null) {
            String name = join(getExceptions(e, predicate));
            String message = messageFunc != null ? messageFunc.apply(e) : e.getMessage();
            if (message != null && message.length() > HEADER_SIZE_LIMIT) {
                message = message.substring(0, HEADER_SIZE_LIMIT);
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
    public static void labelHeaders(Throwable e, BiConsumer<String, String> consumer) {
        labelHeaders(e, null, null, consumer);
    }

    /**
     * Generates exception-related HTTP headers from a Throwable object.
     *
     * @param e           the Throwable object to generate headers from
     * @param messageFunc message generator (uses getMessage() if null)
     * @param consumer    a BiConsumer to accept the generated headers
     */
    public static void labelHeaders(Throwable e, Function<Throwable, String> messageFunc, BiConsumer<String, String> consumer) {
        labelHeaders(e, messageFunc, null, consumer);
    }

    /**
     * Generates exception-related HTTP headers from a Throwable object.
     *
     * @param e         the Throwable object to generate headers from
     * @param predicate a Predicate to filter the exception names
     * @param consumer  a BiConsumer to accept the generated headers
     */
    public static void labelHeaders(Throwable e, Predicate<Throwable> predicate, BiConsumer<String, String> consumer) {
        labelHeaders(e, null, predicate, consumer);
    }

    /**
     * Generates HTTP headers from a Throwable with URL-encoded message.
     *
     * @param e           Throwable source (nullable)
     * @param messageFunc message generator (uses getMessage() if null)
     * @param predicate   exception filter (defaults to non-wrapped)
     * @param consumer    receives headers: names and encoded message
     */
    public static void labelHeaders(Throwable e,
                                    Function<Throwable, String> messageFunc,
                                    Predicate<Throwable> predicate,
                                    BiConsumer<String, String> consumer) {
        describe(e, messageFunc, predicate == null ? ExceptionUtils::isNotWrapped : predicate, (name, message) -> {
            if (name != null && !name.isEmpty()) {
                consumer.accept(LABEL_EXCEPTION_NAMES, name);
            }
            if (message != null && !message.isEmpty()) {
                try {
                    String encodeMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.name());
                    consumer.accept(LABEL_EXCEPTION_MESSAGE, encodeMessage);
                } catch (UnsupportedEncodingException ignore) {
                }
            }
        });
    }

}
