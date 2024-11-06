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

import com.fasterxml.jackson.annotation.JsonFormat.Value;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.jd.live.agent.core.exception.ParseException;
import com.jd.live.agent.core.parser.json.*;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * JsonAnnotationIntrospector is a custom JacksonAnnotationIntrospector that provides additional
 * functionality for handling custom annotations and converters.
 */
public class JsonAnnotationIntrospector extends JacksonAnnotationIntrospector {

    /**
     * A map of custom converters keyed by their respective classes.
     */
    protected Map<Class<?>, JacksonConverter<?, ?>> converters = new ConcurrentHashMap<>();

    @Override
    public Object findDeserializationConverter(Annotated a) {
        JacksonConverter<?, ?> converter = getConverter(a, DeserializeConverter.class, DeserializeConverter::value);
        return converter != null ? converter : super.findDeserializationConverter(a);
    }

    /**
     * Retrieves a custom converter based on the specified annotation type and function.
     *
     * @param <T>            the type of the annotation.
     * @param a              the annotated element.
     * @param annotationType the class of the annotation to look for.
     * @param func           a function to extract the converter class from the annotation.
     * @return the custom converter, or null if none is found.
     */
    protected <T extends Annotation> JacksonConverter<?, ?> getConverter(Annotated a,
                                                                         Class<T> annotationType,
                                                                         Function<T, Class<?>> func) {
        T annotation = a.getAnnotation(annotationType);
        if (annotation != null) {
            return converters.computeIfAbsent(func.apply(annotation), type -> {
                try {
                    return new JacksonConverter<>((JsonConverter<?, ?>) type.newInstance());
                } catch (Throwable e) {
                    throw new ParseException("an error occurred while parsing data.", e);
                }
            });
        }
        return null;
    }

    @Override
    public Object findSerializationConverter(Annotated a) {
        JacksonConverter<?, ?> converter = getConverter(a, SerializeConverter.class, SerializeConverter::value);
        return converter != null ? converter : super.findSerializationConverter(a);
    }

    @Override
    public Value findFormat(Annotated ann) {
        JsonFormat format = ann.getAnnotation(JsonFormat.class);
        if (format != null) {
            String pattern = format.pattern();
            if (pattern.isEmpty()) {
                return null;
            }
            return Value.forPattern(pattern);
        }
        return null;
    }

    @Override
    public List<PropertyName> findPropertyAliases(Annotated a) {
        JsonAlias alias = a.getAnnotation(JsonAlias.class);
        if (alias != null) {
            String[] names = alias.value();
            List<PropertyName> result = new ArrayList<>(names.length);
            for (String name : names) {
                if (name != null && !name.isEmpty()) {
                    result.add(PropertyName.construct(name));
                }
            }
            return result;
        }
        return null;
    }

    @Override
    public PropertyName findNameForSerialization(Annotated a) {
        PropertyName field = getJsonField(a);
        return field != null ? field : super.findNameForSerialization(a);
    }

    @Override
    public PropertyName findNameForDeserialization(Annotated a) {
        PropertyName field = getJsonField(a);
        return field != null ? field : super.findNameForSerialization(a);
    }

    @Override
    public void findEnumAliases(MapperConfig<?> config, AnnotatedClass annotatedClass, Enum<?>[] enumValues, String[][] aliasList) {
        HashMap<String, String[]> enumToAliasMap = new HashMap<>();
        for (AnnotatedField field : annotatedClass.fields()) {
            JsonAlias alias = field.getAnnotation(JsonAlias.class);
            if (alias != null) {
                enumToAliasMap.putIfAbsent(field.getName(), alias.value());
            }
        }

        for (int i = 0, end = enumValues.length; i < end; ++i) {
            Enum<?> enumValue = enumValues[i];
            aliasList[i] = enumToAliasMap.getOrDefault(enumValue.name(), new String[]{});
        }
    }

    /**
     * Retrieves the JSON field name for the given annotated element based on the JsonField annotation.
     *
     * @param a the annotated element.
     * @return the property name, or null if no relevant annotation is found.
     */
    protected PropertyName getJsonField(Annotated a) {
        JsonField field = a.getAnnotation(JsonField.class);
        if (field != null) {
            String name = field.value();
            if (!name.isEmpty()) {
                return PropertyName.construct(field.value());
            }
        }
        return null;
    }
}
