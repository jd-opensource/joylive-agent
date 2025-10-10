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
package com.jd.live.agent.core.inject.jbind.converter.supplier;

import com.jd.live.agent.core.inject.annotation.CaseInsensitive;
import com.jd.live.agent.core.inject.jbind.Converter;
import com.jd.live.agent.core.util.map.CaseInsensitiveMap;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * {@code AbstractMapConverter} is an abstract base class that provides a method to create instances of various
 * {@link Map} types. It implements the {@link Converter} interface, which should define the conversion logic
 * for specific use cases.
 *
 * <p>This class contains a protected method {@link #createMap(Field, Class)} that instantiates different implementations
 * of {@link Map} based on the provided type.</p>
 *
 * @see Converter
 */
public abstract class AbstractMapConverter implements Converter {

    /**
     * Creates an instance of a {@link Map} based on the provided type.
     * <p>
     * This method supports the following types:
     * <ul>
     *     <li>{@link Map}</li>
     *     <li>{@link ConcurrentMap}</li>
     *     <li>{@link SortedMap}</li>
     *     <li>{@link NavigableMap}</li>
     *     <li>{@link ConcurrentNavigableMap}</li>
     * </ul>
     * If the provided type is not an interface and is a concrete class, it attempts to instantiate it using reflection.
     *
     * @param type the class type of the map to create.
     * @return a new instance of the specified map type, or {@code null} if the type is an interface that is not supported.
     * @throws Exception if there is an error creating the map instance.
     */
    protected Map createMap(Field field, Class<?> type) throws Exception {
        if (Map.class == type) {
            CaseInsensitive insensitive = field == null ? null : field.getAnnotation(CaseInsensitive.class);
            return insensitive == null || !insensitive.value() ? new HashMap<>() : new CaseInsensitiveMap();
        } else if (ConcurrentMap.class == type) {
            return new ConcurrentHashMap<>();
        } else if (SortedMap.class == type) {
            return new TreeMap<>();
        } else if (NavigableMap.class == type) {
            return new TreeMap<>();
        } else if (ConcurrentNavigableMap.class == type) {
            return new ConcurrentSkipListMap<>();
        } else if (!type.isInterface()) {
            return (Map) type.newInstance();
        }
        return null;
    }

}
