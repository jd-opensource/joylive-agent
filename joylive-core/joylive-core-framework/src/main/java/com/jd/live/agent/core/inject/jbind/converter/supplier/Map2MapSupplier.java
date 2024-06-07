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
import com.jd.live.agent.core.inject.jbind.*;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

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

    public static class Map2MapConverter extends AbstractMapConverter {
        protected static final Converter INSTANCE = new Map2MapConverter();

        @SuppressWarnings("unchecked")
        @Override
        public Object convert(final Conversion conversion) throws Exception {
            TypeInfo typeInfo = conversion.getTargetType();
            Class<?> targetClass = typeInfo.getRawType();
            Map result = createMap(targetClass);
            if (result != null) {
                Type type = typeInfo.getType();
                if (type instanceof ParameterizedType) {
                    // parameterized conversion
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (actualTypeArguments != null
                            && actualTypeArguments.length == 2
                            && actualTypeArguments[0] instanceof Class
                            && actualTypeArguments[1] instanceof Class) {
                        Map<?, ?> source = (Map<?, ?>) conversion.getSource();
                        TypeInfo targetKeyType = new TypeInfo((Class<?>) actualTypeArguments[0]);
                        TypeInfo targetValueType = new TypeInfo((Class<?>) actualTypeArguments[1]);
                        for (Map.Entry<?, ?> entry : source.entrySet()) {
                            Object key = entry.getKey();
                            Object value = entry.getValue();
                            TypeInfo srcKeyType = new TypeInfo(key.getClass());
                            TypeInfo srcValueType = new TypeInfo(value.getClass());
                            Converter keyConverter = conversion.getConverter(new ConversionType(srcKeyType, targetKeyType));
                            Converter valueConverter = conversion.getConverter(new ConversionType(srcValueType, targetValueType));
                            Conversion keyConversion = conversion.of(srcKeyType, targetKeyType, key);
                            Conversion valueConversion = conversion.of(srcValueType, targetValueType, value);
                            result.put(keyConverter.convert(keyConversion), valueConverter.convert(valueConversion));
                        }
                        return result;
                    }
                }
                result.putAll((Map<?, ?>) conversion.getSource());
            }
            return result;
        }
    }
}
