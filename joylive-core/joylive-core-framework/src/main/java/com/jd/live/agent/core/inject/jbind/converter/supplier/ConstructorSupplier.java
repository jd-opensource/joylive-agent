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
import com.jd.live.agent.core.util.type.ClassDesc;
import com.jd.live.agent.core.util.type.ClassUtils;

import java.lang.reflect.Constructor;

@Extension(value = "ConstructorSupplier", order = ConverterSupplier.CONSTRUCTOR_SUPPLIER_ORDER)
public class ConstructorSupplier implements ConverterSupplier {
    @Override
    public Converter getConverter(ConversionType type) {
        ClassDesc classDesc = ClassUtils.describe(type.getTargetType().getRawType());
        Constructor<?> constructor = classDesc.getConstructorList().getConstructor(type.getSourceType().getRawType());
        return constructor == null ? null : new ConstructorConverter(constructor);
    }

    public static class ConstructorConverter implements Converter {
        protected final Constructor<?> constructor;

        public ConstructorConverter(Constructor<?> constructor) {
            if (!constructor.isAccessible())
                constructor.setAccessible(true);
            this.constructor = constructor;
        }

        @Override
        public Object convert(final Conversion conversion) throws Exception {
            return conversion == null ? null : constructor.newInstance(conversion.getSource());
        }
    }
}
