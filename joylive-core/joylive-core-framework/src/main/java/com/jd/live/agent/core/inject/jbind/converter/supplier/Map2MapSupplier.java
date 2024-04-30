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

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.jbind.Conversion;
import com.jd.live.agent.core.inject.jbind.ConversionType;
import com.jd.live.agent.core.inject.jbind.Converter;
import com.jd.live.agent.core.inject.jbind.ConverterSupplier;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

@Extension(value = "Map2MapSupplier", order = ConverterSupplier.MAP_TO_OBJECT_ORDER)
public class Map2MapSupplier implements ConverterSupplier {
    @Override
    public Converter getConverter(ConversionType type) {
        Class<?> targetType = type.getTargetType().getRawType();
        Class<?> sourceType = type.getSourceType().getRawType();
        if (!Map.class.isAssignableFrom(sourceType)
                || !Map.class.isAssignableFrom(targetType)
                || (!targetType.isInterface() && Modifier.isAbstract(targetType.getModifiers()))) {
            return null;
        }
        return Map2MapConverter.INSTANCE;
    }

    public static class Map2MapConverter implements Converter {
        protected static final Converter INSTANCE = new Map2MapConverter();

        @Override
        public Object convert(final Conversion conversion) throws Exception {
            Map<Object, Object> result = null;
            Class<?> targetType = conversion.getTargetType().getRawType();
            if (Map.class == targetType) {
                result = new HashMap<>();
            } else if (ConcurrentMap.class == targetType) {
                result = new ConcurrentHashMap<>();
            } else if (SortedMap.class == targetType) {
                result = new TreeMap<>();
            } else if (NavigableMap.class == targetType) {
                result = new TreeMap<>();
            } else if (ConcurrentNavigableMap.class == targetType) {
                result = new ConcurrentSkipListMap<>();
            } else if (!targetType.isInterface()) {
                result = (Map<Object, Object>) targetType.newInstance();
            }
            if (result != null) {
                result.putAll((Map<Object, Object>) conversion.getSource());
            }
            return result;
        }
    }
}
