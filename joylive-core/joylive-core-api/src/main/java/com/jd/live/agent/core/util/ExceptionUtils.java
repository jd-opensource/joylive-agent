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

import com.jd.live.agent.core.exception.WrappedException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A utility class for working with exceptions.
 */
public class ExceptionUtils {

    public static final Function<Throwable, IOException> IO_EXCEPTION_CONVERTER = e -> e instanceof IOException ? (IOException) e : new IOException(e.getMessage(), e);

    /**
     * Iterates over the exception chain starting from the given throwable and stops when the provided predicate returns false.
     *
     * @param e         the throwable to start iterating from
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
     * Checks if the given throwable is not a wrapped exception.
     *
     * @param e The throwable to check.
     * @return {@code true} if the throwable is not a wrapped exception, {@code false} otherwise.
     */
    public static boolean isNoneWrapped(Throwable e) {
        return !(e instanceof WrappedException || e instanceof InvocationTargetException || e instanceof ExecutionException);
    }

    /**
     * Gets the cause of the given throwable.
     *
     * @param e the throwable to examine
     * @return the root cause if available, or the original throwable
     */
    public static Throwable getCause(Throwable e) {
        Throwable cause = null;
        if (e instanceof InvocationTargetException) {
            cause = e.getCause();
        } else if (e instanceof ExecutionException) {
            cause = e.getCause();
        }
        return cause == null ? e : cause;
    }

    /**
     * Finds the root cause of a throwable, optionally filtered by a predicate.
     * Traverses the cause chain until either the end or the predicate rejects a cause.
     *
     * @param e         the throwable to analyze (may be null)
     * @param predicate optional filter to test each cause (may be null to accept all)
     * @return the deepest acceptable cause in the chain, or null if rejected by predicate
     */
    public static Throwable getRootCause(Throwable e, Predicate<Throwable> predicate) {
        Throwable cause = null;
        while (e != null && (predicate == null || predicate.test(e))) {
            cause = e;
            e = e.getCause();
        }
        return cause;
    }

}
