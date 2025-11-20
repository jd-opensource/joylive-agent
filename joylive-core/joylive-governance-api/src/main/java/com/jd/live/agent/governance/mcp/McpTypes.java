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
package com.jd.live.agent.governance.mcp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.time.temporal.Temporal;
import java.util.*;

public class McpTypes {

    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_ARRAY = "array";
    public static final String TYPE_OBJECT = "object";
    public static final String TYPE_NULL = "null";

    public static final String FORMAT_INT32 = "int32";
    public static final String FORMAT_INT64 = "int64";
    public static final String FORMAT_FLOAT = "float";
    public static final String FORMAT_DOUBLE = "double";
    public static final String FORMAT_BINARY = "binary";
    public static final String FORMAT_UUID = "uuid";
    public static final String FORMAT_DATE_TIME = "date-time";
    public static final String FORMAT_NONE = null;

    private static final Map<String, String> OPEN_API_TYPE_MAPPING = new HashMap<>();
    private static final Map<String, String> OPEN_API_FORMAT_MAPPING = new HashMap<>();

    static {
        OPEN_API_TYPE_MAPPING.put("string", TYPE_STRING);
        OPEN_API_TYPE_MAPPING.put("number", TYPE_NUMBER);
        OPEN_API_TYPE_MAPPING.put("integer", TYPE_NUMBER);
        OPEN_API_TYPE_MAPPING.put("boolean", TYPE_BOOLEAN);
        OPEN_API_TYPE_MAPPING.put("array", TYPE_ARRAY);
        OPEN_API_TYPE_MAPPING.put("object", TYPE_OBJECT);
        OPEN_API_TYPE_MAPPING.put("null", TYPE_NULL);

        OPEN_API_FORMAT_MAPPING.put("int32", FORMAT_INT32);
        OPEN_API_FORMAT_MAPPING.put("int64", FORMAT_INT64);
        OPEN_API_FORMAT_MAPPING.put("float", FORMAT_FLOAT);
        OPEN_API_FORMAT_MAPPING.put("double", FORMAT_DOUBLE);
        OPEN_API_FORMAT_MAPPING.put("binary", FORMAT_BINARY);
        OPEN_API_FORMAT_MAPPING.put("uuid", FORMAT_UUID);
        OPEN_API_FORMAT_MAPPING.put("date-time", FORMAT_DATE_TIME);
    }

    /**
     * Determines the type and format for a given Java class.
     *
     * @param type The Java class to analyze
     * @return A TypeFormat object containing the corresponding type and format
     */
    public static TypeFormat getTypeFormat(Class<?> type) {
        if (type == null || void.class == type || Void.class == type) {
            return new TypeFormat(TYPE_NULL, FORMAT_NONE);
        } else if (String.class == type || char.class == type || Character.class == type) {
            return new TypeFormat(TYPE_STRING, FORMAT_NONE);
        } else if (int.class == type || Integer.class == type) {
            return new TypeFormat(TYPE_NUMBER, FORMAT_INT32);
        } else if (long.class == type || Long.class == type) {
            return new TypeFormat(TYPE_NUMBER, FORMAT_INT64);
        } else if (float.class == type || Float.class == type) {
            return new TypeFormat(TYPE_NUMBER, FORMAT_FLOAT);
        } else if (double.class == type || Double.class == type) {
            return new TypeFormat(TYPE_NUMBER, FORMAT_DOUBLE);
        } else if (byte.class == type || Byte.class == type || short.class == type || Short.class == type) {
            return new TypeFormat(TYPE_NUMBER, FORMAT_INT32);
        } else if (Number.class.isAssignableFrom(type)) {
            return new TypeFormat(TYPE_NUMBER, FORMAT_NONE);
        } else if (boolean.class == type || Boolean.class == type) {
            return new TypeFormat(TYPE_BOOLEAN, FORMAT_NONE);
        } else if (Date.class.isAssignableFrom(type) || Temporal.class.isAssignableFrom(type)) {
            return new TypeFormat(TYPE_STRING, FORMAT_DATE_TIME);
        } else if (byte[].class == type) {
            return new TypeFormat(TYPE_STRING, FORMAT_BINARY);
        } else if (type.isArray() || Collection.class.isAssignableFrom(type)) {
            return new TypeFormat(TYPE_ARRAY, FORMAT_NONE);
        } else if (Map.class.isAssignableFrom(type)) {
            return new TypeFormat(TYPE_OBJECT, FORMAT_NONE);
        } else if (type.isEnum()) {
            return new TypeFormat(TYPE_STRING, FORMAT_NONE);
        } else if (UUID.class == type) {
            return new TypeFormat(TYPE_STRING, FORMAT_UUID);
        } else {
            return new TypeFormat(TYPE_OBJECT, FORMAT_NONE);
        }
    }

    /**
     * Converts OpenAPI type and format to standardized TypeFormat.
     *
     * @param type   The OpenAPI type string
     * @param format The OpenAPI format string
     * @return A TypeFormat object with resolved type and format values
     */
    public static TypeFormat getTypeFormat(String type, String format) {
        if (type == null) {
            return new TypeFormat(TYPE_NULL, FORMAT_NONE);
        }
        type = OPEN_API_TYPE_MAPPING.getOrDefault(type, TYPE_OBJECT);
        format = OPEN_API_FORMAT_MAPPING.getOrDefault(format, format);
        return new TypeFormat(type, format);
    }

    @Getter
    @AllArgsConstructor
    @ToString
    public static class TypeFormat {
        private final String type;
        private final String format;

        public boolean isArray() {
            return TYPE_ARRAY.equals(type);
        }

        public boolean isObject() {
            return TYPE_OBJECT.equals(type);
        }
    }
}
