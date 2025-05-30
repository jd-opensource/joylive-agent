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
package com.jd.live.agent.implement.parser.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.jd.live.agent.core.parser.json.JsonType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom JSON serializer that adds type information to serialized objects.
 * <p>
 * Serializes objects in the format:
 * {@code {"@type":"fully.qualified.ClassName","field1":value1,"field2":value2}}
 * where "@type" is configurable through {@link JsonType} annotation.
 * <p>
 * Thread-safe implementation caches property writers for better performance.
 */
public class JsonTypeSerializer extends StdSerializer<Object> {

    private final JsonType jsonType;

    private final Map<Class<?>, List<PropertyWriter>> types = new ConcurrentHashMap<>();

    public JsonTypeSerializer(JavaType javaType, JsonType jsonType) {
        super(javaType);
        this.jsonType = jsonType;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        List<PropertyWriter> writers = getPropertyWriters(value.getClass(), provider);
        gen.writeStartObject();
        gen.writeFieldName(jsonType.name());
        gen.writeString(jsonType.value().isEmpty() ? value.getClass().getName() : jsonType.value());

        for (PropertyWriter prop : writers) {
            try {
                prop.serializeAsField(value, gen, provider);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException("Failed to serialize field " + prop.getName(), e);
            }
        }
        gen.writeEndObject();
    }

    /**
     * Gets cached property writers for given type
     *
     * @param type     Class to inspect
     * @param provider Serializer provider
     * @return List of property writers
     * @throws JsonMappingException If properties cannot be resolved
     */
    private List<PropertyWriter> getPropertyWriters(Class<?> type, SerializerProvider provider) throws JsonMappingException {
        List<PropertyWriter> writers = types.get(type);
        if (writers == null) {
            writers = buildProperties(type, provider);
            List<PropertyWriter> old = types.putIfAbsent(type, writers);
            if (old != null) {
                writers = old;
            }
        }
        return writers;
    }

    /**
     * Builds property writers list for type
     *
     * @param type     Class to inspect
     * @param provider Serializer provider
     * @return New list of property writers
     * @throws JsonMappingException If properties cannot be resolved
     */
    private List<PropertyWriter> buildProperties(Class<?> type, SerializerProvider provider) throws JsonMappingException {
        JsonSerializer<?> serializer = provider.findValueSerializer(type);
        Iterator<PropertyWriter> it = serializer.properties();
        List<PropertyWriter> writers = new ArrayList<>();
        while (it.hasNext()) writers.add(it.next());
        return writers;
    }

}
