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
package com.jd.live.agent.implement.parser.fastjson2;

import com.jd.live.agent.core.exception.ParseException;
import com.jd.live.agent.core.parser.json.JsonConverter;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A utility class for managing and creating JSON converters.
 */
public class Converters {

    /**
     * A map of field names to their corresponding JSON converters.
     */
    private static final Map<String, JsonConverter<?, ?>> FIELD_CONVERTERS = new ConcurrentHashMap<>();

    /**
     * A map of converter class names to their corresponding JSON converters.
     */
    private static final Map<String, JsonConverter<?, ?>> CONVERTERS = new ConcurrentHashMap<>();

    /**
     * Gets or creates a JSON converter instance for the given converter class.
     *
     * @param clazz The converter class to get or create an instance for.
     * @return The JSON converter instance, or null if the given class is null.
     * @throws ParseException If an error occurs while creating the converter instance.
     */
    public static JsonConverter<?, ?> getOrCreateConverter(Class<? extends JsonConverter<?, ?>> clazz) {
        return clazz == null ? null : CONVERTERS.computeIfAbsent(clazz.getName(), key -> {
            try {
                Constructor<? extends JsonConverter<?, ?>> constructor = clazz.getConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (Throwable e) {
                throw new ParseException(e.getMessage(), e);
            }
        });
    }

    /**
     * Gets or creates a JSON converter instance for the given field name and converter class.
     *
     * @param fieldName The field name to get or create a converter instance for.
     * @param clazz     The converter class to get or create an instance for.
     * @return The JSON converter instance, or null if either the field name or the converter class is null.
     */
    public static JsonConverter<?, ?> getOrCreateConverter(String fieldName, Class<? extends JsonConverter<?, ?>> clazz) {
        return fieldName == null || clazz == null ? null : FIELD_CONVERTERS.computeIfAbsent(fieldName, name -> getOrCreateConverter(clazz));
    }

    /**
     * Gets the JSON converter instance for the given field name.
     *
     * @param fieldName The field name to get the converter instance for.
     * @return The JSON converter instance, or null if no converter instance exists for the given field name.
     */
    public static JsonConverter<?, ?> getConverter(String fieldName) {
        return fieldName == null ? null : FIELD_CONVERTERS.get(fieldName);
    }

}
