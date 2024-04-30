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
import com.jd.live.agent.core.inject.Injection;
import com.jd.live.agent.core.inject.jbind.Conversion;
import com.jd.live.agent.core.inject.jbind.ConversionType;
import com.jd.live.agent.core.inject.jbind.Converter;
import com.jd.live.agent.core.inject.jbind.ConverterSupplier;
import com.jd.live.agent.core.util.type.ClassDesc;
import com.jd.live.agent.core.util.type.ClassUtils;

import java.lang.reflect.Constructor;
import java.util.Map;

@Extension(value = "Map2ObjectSupplier", order = ConverterSupplier.MAP_TO_OBJECT_ORDER)
public class Map2ObjectSupplier implements ConverterSupplier {
    @Override
    public Converter getConverter(ConversionType type) {
        Class<?> targetType = type.getTargetType().getRawType();
        Class<?> sourceType = type.getSourceType().getRawType();
        if (!Map.class.isAssignableFrom(sourceType) || !ClassUtils.isEntity(targetType)) {
            return null;
        }
        return Map2ObjectConverter.INSTANCE;
    }

    public static class Map2ObjectConverter implements Converter {
        protected static final Converter INSTANCE = new Map2ObjectConverter();

        @Override
        public Object convert(final Conversion conversion) throws Exception {
            Class<?> targetType = conversion.getTargetType().getInboxType();
            Object obj = null;
            Injection injection = conversion.build(targetType);
            if (injection != null) {
                ClassDesc classDesc = ClassUtils.describe(targetType);
                Constructor<?> constructor = classDesc.getConstructorList().getDefaultConstructor();
                if (constructor == null) {
                    return null;
                }
                obj = constructor.newInstance();
                injection.inject(conversion.getSource(), obj);
            }
            return obj;
        }
    }
}
