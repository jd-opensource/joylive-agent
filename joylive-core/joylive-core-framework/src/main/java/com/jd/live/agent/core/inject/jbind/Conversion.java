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
package com.jd.live.agent.core.inject.jbind;

import com.jd.live.agent.core.inject.Injection;
import com.jd.live.agent.core.inject.jbind.InjectionContext.EmbedInjectionContext;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * The Conversion class extends ConversionType and represents a conversion operation
 * between two types, providing additional context and methods for type conversion.
 * It implements the ConverterSelector and ArrayFactory interfaces to support
 * advanced conversion operations.
 */
public class Conversion extends ConversionType implements ConverterSelector, ArrayFactory {

    @Getter
    private final Field field;

    @Getter
    private final Object source;

    private final EmbedInjectionContext context;

    @Getter
    private final Map<String, Object> components;

    @Setter
    @Getter
    private String path;

    public Conversion(Field field,
                      TypeInfo sourceType,
                      TypeInfo targetType,
                      Object source,
                      EmbedInjectionContext context,
                      Map<String, Object> components) {
        super(sourceType, targetType);
        this.field = field;
        this.source = source;
        this.context = context;
        this.components = components;
    }

    public Conversion(Field field,
                      ConversionType type,
                      Object source,
                      EmbedInjectionContext context,
                      Map<String, Object> components) {
        super(type);
        this.field = field;
        this.source = source;
        this.context = context;
        this.components = components;
    }

    @Override
    public ArrayBuilder getArrayBuilder(Class<?> componentType) {
        return context.getArrayBuilder(componentType);
    }

    @Override
    public Converter getConverter(ConversionType type) {
        return context.getConverter(type);
    }

    /**
     * Builds an Injection for the specified type using the context.
     *
     * @param type the type of object to build the Injection for
     * @return an Injection instance for the specified type
     */
    public Injection build(Class<?> type) {
        return context.build(type);
    }

    /**
     * Creates a new Conversion instance with specified parameters and current context.
     *
     * @param field the field being converted
     * @param sourceType the source type
     * @param targetType the target type
     * @param source the source object
     * @return new Conversion instance
     */
    public Conversion of(Field field, TypeInfo sourceType, TypeInfo targetType, Object source) {
        return new Conversion(field, sourceType, targetType, source, context, components);
    }

    /**
     * Creates a new Conversion instance with specified parameters and current context.
     *
     * @param sourceType the source type
     * @param targetType the target type
     * @param source     the source object
     * @return new Conversion instance
     */
    public Conversion of(TypeInfo sourceType, TypeInfo targetType, Object source) {
        return new Conversion(field, sourceType, targetType, source, context, components);
    }
}
