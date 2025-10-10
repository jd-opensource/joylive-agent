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
import com.jd.live.agent.core.inject.jbind.PolyObject.CollectionPolyObject;
import com.jd.live.agent.core.inject.jbind.converter.Abstract2PolyConverter.Collection2PolyConverter;

@Extension(value = "Collection2CollectionSupplier", order = ConverterSupplier.COLLECTION_TO_COLLECTION_ORDER)
public class Collection2CollectionSupplier implements ConverterSupplier {
    @Override
    public Converter getConverter(ConversionType type) {
        return type.getSourceType().isCollection() && type.getTargetType().isCollection() ? Collection2CollectionConverter.INSTANCE : null;
    }

    public static class Collection2CollectionConverter extends Collection2PolyConverter {

        protected static final Converter INSTANCE = new Collection2CollectionConverter();

        @Override
        protected PolyObject createTargetSet(Conversion conversion, Class<?> componentType, int size) throws Exception {
            return new CollectionPolyObject(createCollection(conversion.getField(), conversion.getTargetType().getRawType(), size));
        }
    }
}
