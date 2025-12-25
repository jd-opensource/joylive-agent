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
import com.jd.live.agent.core.parser.ObjectParser;

import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static com.jd.live.agent.core.util.StringUtils.split;

@Extension(value = "String2MapSupplier", order = ConverterSupplier.STRING_TO_MAP_ORDER)
public class String2MapSupplier implements ConverterSupplier {
    @Override
    public Converter getConverter(ConversionType type) {
        Class<?> targetType = type.getTargetType().getRawType();
        if (!type.getSourceType().isCharSequence()
                || !Map.class.isAssignableFrom(targetType)
                || (!targetType.isInterface() && Modifier.isAbstract(targetType.getModifiers()))) {
            return null;
        }
        return String2MapConverter.INSTANCE;
    }

    private static class String2MapConverter extends AbstractMapConverter {

        private static final Converter INSTANCE = new String2MapConverter();

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public Object convert(Conversion conversion) throws Exception {
            TypeInfo typeInfo = conversion.getTargetType();
            Class<?> targetClass = typeInfo.getRawType();
            Map result = createMap(conversion.getField(), targetClass);
            if (result == null) {
                return null;
            }
            String value = conversion.getSource().toString();
            if (value == null || value.isEmpty()) {
                return result;
            } else if (value.startsWith("{") && value.endsWith("}")) {
                // Support standard JSON format
                ObjectParser parser = conversion.getComponent(ObjectParser.COMPONENT_JSON);
                result = parser.read(new StringReader(value), typeInfo.getType());
            } else {
                String[] parts = split(value, ';');
                for (String part : parts) {
                    int pos = part.indexOf('=');
                    // TODO convert key and value
                    if (pos > 0) {
                        result.put(part.substring(0, pos), part.substring(pos + 1));
                    } else if (pos == -1 && !(result instanceof ConcurrentMap)) {
                        result.put(part, null);
                    }
                }
            }
            return result;
        }

    }
}
