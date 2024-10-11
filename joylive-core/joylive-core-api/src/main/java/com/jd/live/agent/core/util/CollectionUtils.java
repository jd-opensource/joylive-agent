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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * CollectionUtils
 */
public class CollectionUtils {

    /**
     * Filters the provided list of objects based on the given predicate.
     *
     * @param <T>       the type of objects in the list.
     * @param objects   the list of objects to be filtered.
     * @param predicate a predicate to test if a message passes the check.
     */
    public static <T> void filter(List<T> objects, Predicate<T> predicate) {
        int size = objects == null ? 0 : objects.size();
        if (size == 0) {
            return;
        }

        int writeIndex = 0;
        for (int readIndex = 0; readIndex < size; readIndex++) {
            T message = objects.get(readIndex);
            if (predicate == null || predicate.test(message)) {
                if (writeIndex != readIndex) {
                    objects.set(writeIndex, message);
                }
                writeIndex++;
            }
        }

        // Remove the remaining elements if any
        if (writeIndex < size) {
            objects.subList(writeIndex, size).clear();
        }
    }

    /**
     * Converts a list of source objects into a list of target objects using the provided converter function.
     *
     * @param <S>       The type of source objects.
     * @param <T>       The type of target objects.
     * @param sources   The list of source objects to convert.
     * @param converter The function to convert each source object into a target object.
     * @return A list of target objects.
     */
    public static <S, T> List<T> convert(List<S> sources, Function<S, T> converter) {
        if (sources == null || converter == null) {
            return new ArrayList<>();
        }
        List<T> result = new ArrayList<>(sources.size());
        for (S instance : sources) {
            result.add(converter.apply(instance));
        }
        return result;
    }
}
