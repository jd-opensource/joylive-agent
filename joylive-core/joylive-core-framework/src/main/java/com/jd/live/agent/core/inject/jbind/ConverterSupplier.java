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

import com.jd.live.agent.core.extension.annotation.Extensible;

/**
 * The ConverterSupplier interface provides a mechanism for supplying converters for
 * complex types that are not covered by fundamental conversions. It defines a method
 * for retrieving a converter based on the conversion type required.
 */
@Extensible("ConverterSupplier")
public interface ConverterSupplier {

    /**
     * A constant that represents the order value for constructor suppliers.
     */
    int CONSTRUCTOR_SUPPLIER_ORDER = 2;

    /**
     * A constant that represents the order value for value-of suppliers.
     */
    int VALUE_OF_SUPPLIER_ORDER = 10;

    /**
     * A constant that represents the order value for from-string suppliers.
     */
    int FROM_STRING_SUPPLIER_ORDER = 10;

    /**
     * A constant that represents the order value for map-to-object suppliers.
     */
    int MAP_TO_OBJECT_ORDER = 50;

    /**
     * A constant that represents the order value for string-to-array suppliers.
     */
    int STRING_TO_ARRAY_ORDER = 70;

    /**
     * A constant that represents the order value for string-to-collection suppliers.
     */
    int STRING_TO_COLLECTION_ORDER = 71;

    /**
     * A constant that represents the order value for string-to-map suppliers.
     */
    int STRING_TO_MAP_ORDER = 72;

    /**
     * A constant that represents the order value for array-to-array suppliers.
     */
    int ARRAY_TO_ARRAY_ORDER = 80;

    /**
     * A constant that represents the order value for array-to-collection suppliers.
     */
    int ARRAY_TO_COLLECTION_ORDER = 81;

    /**
     * A constant that represents the order value for collection-to-array suppliers.
     */
    int COLLECTION_TO_ARRAY_ORDER = 82;

    /**
     * A constant that represents the order value for collection-to-collection suppliers.
     */
    int COLLECTION_TO_COLLECTION_ORDER = 83;

    /**
     * A constant that represents the order value for to-string suppliers.
     */
    int TO_STRING_ORDER = 99;

    /**
     * Retrieves a converter for the specified conversion type.
     *
     * @param type the ConversionType that defines the source and target types for the conversion
     * @return a Converter instance capable of performing the conversion from the source type to the target type
     */
    Converter getConverter(ConversionType type);
}

