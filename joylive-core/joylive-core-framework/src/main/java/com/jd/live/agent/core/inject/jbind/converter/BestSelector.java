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
package com.jd.live.agent.core.inject.jbind.converter;

import com.jd.live.agent.core.inject.jbind.*;
import com.jd.live.agent.core.inject.jbind.Converter.FundamentalConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A selector that finds the best matching converter for a given conversion type and provides array builders.
 */
public class BestSelector implements ConverterSelector, ArrayFactory {

    private final Map<Class<?>, Map<Class<?>, Converter>> converts = new ConcurrentHashMap<>(1000);
    private final Map<Class<?>, ArrayBuilder> arrays = new HashMap<>();
    private final List<ConverterSupplier> converterSuppliers = new ArrayList<>();

    /**
     * Constructs a BestSelector with specified converter suppliers, fundamental converters, and array builders.
     *
     * @param converterSuppliers    List of additional converter suppliers.
     * @param fundamentalConverters List of fundamental converters.
     * @param arrayBuilders         List of array builders.
     */
    public BestSelector(List<ConverterSupplier> converterSuppliers,
                        List<FundamentalConverter> fundamentalConverters,
                        List<ArrayBuilder> arrayBuilders) {
        this.converterSuppliers.add(new FundamentalSupplier(fundamentalConverters));
        if (converterSuppliers != null) {
            this.converterSuppliers.addAll(converterSuppliers);
        }
        for (ArrayBuilder supplier : arrayBuilders) {
            arrays.put(supplier.getComponentType(), supplier);
        }
    }

    /**
     * Retrieves a converter for the specified conversion type.
     *
     * @param type The conversion type.
     * @return The converter that can handle the conversion.
     */
    @Override
    public Converter getConverter(ConversionType type) {
        return converts.computeIfAbsent(type.getTargetType().getInboxType(), t -> new ConcurrentHashMap<>()).
                computeIfAbsent(type.getSourceType().getInboxType(), t -> getComplexConverter(type));
    }

    /**
     * Obtains a complex converter for the specified conversion type.
     *
     * @param type The conversion type.
     * @return The complex converter.
     */
    protected Converter getComplexConverter(ConversionType type) {
        TypeInfo targetType = type.getTargetType();
        Class<?> targetInboxType = targetType.getInboxType();
        TypeInfo sourceType = type.getSourceType();
        Class<?> sourceInboxType = sourceType.getInboxType();
        if (((targetType.isPrimitive() || targetType.isEnum())) &&
                (targetInboxType == sourceInboxType || targetInboxType.isAssignableFrom(sourceInboxType))) {
            // Direct assignment is possible
            return Converter.NONE;
        } else {
            Converter converter;
            for (ConverterSupplier supplier : converterSuppliers) {
                converter = supplier.getConverter(type);
                if (converter != null) {
                    return converter;
                }
            }
            return Converter.ERROR;
        }
    }

    /**
     * Retrieves an array builder for the specified component type.
     *
     * @param componentType The component type of the array.
     * @return The array builder for the component type, if available.
     */
    @Override
    public ArrayBuilder getArrayBuilder(Class<?> componentType) {
        return arrays.get(componentType);
    }

}

