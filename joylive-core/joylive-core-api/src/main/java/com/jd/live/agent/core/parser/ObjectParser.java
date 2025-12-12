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
package com.jd.live.agent.core.parser;

import com.jd.live.agent.core.extension.annotation.Extensible;

import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;

/**
 * Defines the contract for parsers that can serialize and deserialize objects to and from different formats
 * such as JSON and YAML. This interface supports extensibility, allowing implementations to handle various
 * data formats for object serialization and deserialization.
 *
 * @since 1.0.0
 */
@Extensible("ObjectParser")
public interface ObjectParser {

    /**
     * Represents the identifier for YAML format.
     */
    String YAML = "yaml";

    /**
     * Represents the identifier for YAML format.
     */
    String YML = "yml";

    /**
     * Represents the identifier for JSON format.
     */
    String JSON = "json";

    /**
     * Reads and deserializes data from the provided {@link Reader} into an object of the specified class.
     *
     * @param <T>    The type of the object to be returned.
     * @param reader The reader from which the data is read.
     * @param clazz  The class of the object to be deserialized into.
     * @return The deserialized object of type {@code T}.
     */
    <T> T read(Reader reader, Class<T> clazz);

    /**
     * Reads and deserializes data from the provided {@link Reader} into an object of the specified type,
     * using a {@link TypeReference} to specify generic type information.
     *
     * @param <T>       The type of the object to be returned.
     * @param reader    The reader from which the data is read.
     * @param reference A {@link TypeReference} specifying the type of the object to be deserialized into.
     * @return The deserialized object of type {@code T}.
     */
    <T> T read(Reader reader, TypeReference<T> reference);

    /**
     * Reads the data from the given {@link Reader} and deserializes it into an object of the specified type.
     *
     * @param <T>    the type of the desired object
     * @param reader the {@link Reader} to read the JSON data from
     * @param type   the {@link Type} of the desired object
     * @return an object of type {@code T} deserialized from the JSON data
     */
    <T> T read(Reader reader, Type type);

    /**
     * Serializes the provided object into a specified format and writes it using the given {@link Writer}.
     *
     * @param writer The writer to which the serialized data is written.
     * @param obj    The object to be serialized.
     */
    void write(Writer writer, Object obj);

    default String write(Object obj) {
        StringWriter writer = new StringWriter(256);
        write(writer, obj);
        return writer.toString();
    }
}
