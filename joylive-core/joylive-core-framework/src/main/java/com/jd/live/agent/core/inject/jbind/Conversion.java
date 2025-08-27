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

import java.util.Map;

/**
 * The Conversion class extends ConversionType and represents a conversion operation
 * between two types, providing additional context and methods for type conversion.
 * It implements the ConverterSelector and ArrayFactory interfaces to support
 * advanced conversion operations.
 */
public class Conversion extends ConversionType implements ConverterSelector, ArrayFactory {

    /**
     * The source object to be converted.
     */
    @Getter
    private final Object source;

    /**
     * The context for embeddable injections.
     */
    private final EmbedInjectionContext context;

    @Getter
    private final Map<String, Object> components;

    /**
     * The path associated with the conversion operation.
     */
    @Setter
    @Getter
    private String path;

    /**
     * Constructs a new Conversion with the specified source and target types,
     * source object, and context.
     *
     * @param sourceType the source type of the conversion
     * @param targetType the target type of the conversion
     * @param source the object to be converted
     * @param context the context for embeddable injections
     */
    public Conversion(TypeInfo sourceType, TypeInfo targetType, Object source, EmbedInjectionContext context, Map<String, Object> components) {
        super(sourceType, targetType);
        this.source = source;
        this.context = context;
        this.components = components;
    }

    /**
     * Constructs a new Conversion by copying an existing ConversionType and
     * specifying the source object and context.
     *
     * @param type the existing ConversionType to copy
     * @param source the object to be converted
     * @param context the context for embeddable injections
     */
    public Conversion(ConversionType type, Object source, EmbedInjectionContext context, Map<String, Object> components) {
        super(type);
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
     * Creates a new Conversion instance with the specified source and target types
     * and the same context as this instance.
     *
     * @param sourceType the source type of the new conversion
     * @param targetType the target type of the new conversion
     * @param source the object to be converted
     * @return a new Conversion instance
     */
    public Conversion of(TypeInfo sourceType, TypeInfo targetType, Object source) {
        return new Conversion(sourceType, targetType, source, context, components);
    }
}
