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
package com.jd.live.agent.core.util.type;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A utility class for scanning the class hierarchy of a given type.
 *
 * @since 1.0.0
 */
public class TypeScanner {

    /**
     * A predicate that checks if a class is not a primitive, annotation, enum, interface, array, or Object.
     */
    public static final Predicate<Class<?>> UNTIL_OBJECT = t ->
            !t.isPrimitive()
                    && !t.isAnnotation()
                    && !t.isEnum()
                    && !t.isInterface()
                    && !t.isArray()
                    && !t.equals(Object.class);

    private final Class<?> type;

    private final Predicate<Class<?>> predicate;

    /**
     * Creates a new TypeScanner instance for the specified type.
     *
     * @param type the type to scan
     */
    private TypeScanner(Class<?> type) {
        this(type, null);
    }

    /**
     * Creates a new TypeScanner instance for the specified type and predicate.
     *
     * @param type     the type to scan
     * @param predicate the predicate to use for filtering classes
     */
    private TypeScanner(Class<?> type, Predicate<Class<?>> predicate) {
        this.type = type;
        this.predicate = predicate == null ? UNTIL_OBJECT : predicate;
    }

    /**
     * Scans the class hierarchy of the specified type and applies the provided consumer to each class that passes the predicate test.
     *
     * @param consumer the consumer to apply to each class
     */
    public void scan(Consumer<Class<?>> consumer) {
        if (type == null || consumer == null)
            return;
        Class<?> clazz = type;
        while (clazz != null && predicate.test(clazz)) {
            consumer.accept(clazz);
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Creates a new TypeScanner instance for the specified type.
     *
     * @param type the type to scan
     * @return a new TypeScanner instance
     */
    public static TypeScanner scanner(Class<?> type) {
        return new TypeScanner(type);
    }

    /**
     * Creates a new TypeScanner instance for the specified type and predicate.
     *
     * @param type     the type to scan
     * @param predicate the predicate to use for filtering classes
     * @return a new TypeScanner instance
     */
    public static TypeScanner scanner(Class<?> type, Predicate<Class<?>> predicate) {
        return new TypeScanner(type, predicate);
    }
}
