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
import com.jd.live.agent.core.inject.jbind.converter.Abstract2PolyConverter;

@Extension(value = "Array2ArraySupplier", order = ConverterSupplier.ARRAY_TO_ARRAY_ORDER)
public class Array2ArraySupplier implements ConverterSupplier {
    @Override
    public Converter getConverter(ConversionType type) {
        return type.getSourceType().isArray() && type.getTargetType().isArray() ?
                Array2ArrayConverter.INSTANCE :
                null;
    }

    public static class Array2ArrayConverter extends Abstract2PolyConverter.Array2PolyConverter {

        protected static final Converter INSTANCE = new Array2ArrayConverter();

        @Override
        protected PolyObject createTargetSet(Conversion conversion, Class<?> componentType, int size) throws Exception {
            return new PolyObject.ArrayPolyObject(conversion.getArrayBuilder(componentType).create(size));
        }

        @Override
        protected boolean isAssignable(Class<?> targetComponentType, Class<?> sourceComponentType) {
            return targetComponentType.equals(sourceComponentType)
                    || targetComponentType.isAssignableFrom(sourceComponentType);
        }
    }
}
