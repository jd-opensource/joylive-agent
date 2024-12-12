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
 * FieldScanner
 *
 * @since 1.0.0
 */
public class TypeScanner {

    public static final Predicate<Class<?>> UNTIL_OBJECT = t ->
            !t.isPrimitive()
                    && !t.isAnnotation()
                    && !t.isEnum()
                    && !t.isInterface()
                    && !t.isArray()
                    && !t.equals(Object.class);

    public static final Predicate<Class<?>> ENTITY_PREDICATE = t ->
            !t.isPrimitive()
                    && !t.isAnnotation()
                    && !t.isEnum()
                    && !t.isInterface()
                    && !t.isArray()
                    && !t.equals(Object.class)
                    && !t.getName().startsWith("java.")
                    && !t.getName().startsWith("javax.");

    private final Class<?> type;

    private final Predicate<Class<?>> predicate;

    public TypeScanner(Class<?> type) {
        this(type, null);
    }

    public TypeScanner(Class<?> type, Predicate<Class<?>> predicate) {
        this.type = type;
        this.predicate = predicate == null ? UNTIL_OBJECT : predicate;
    }

    public void scan(Consumer<Class<?>> consumer) {
        if (type == null || consumer == null)
            return;
        Class<?> clazz = type;
        while (clazz != null && predicate.test(clazz)) {
            consumer.accept(clazz);
            clazz = clazz.getSuperclass();
        }
    }

    public static TypeScanner build(Class<?> type) {
        return new TypeScanner(type);
    }

    public static TypeScanner build(Class<?> type, Predicate<Class<?>> predicate) {
        return new TypeScanner(type, predicate);
    }
}
