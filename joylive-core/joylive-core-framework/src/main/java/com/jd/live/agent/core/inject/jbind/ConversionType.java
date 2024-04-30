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

import lombok.Getter;

/**
 * The ConversionType class represents a conversion between two types, encapsulating
 * the source and target types of the conversion.
 */
@Getter
public class ConversionType {

    /**
     * The type information of the source type in the conversion.
     */
    protected final TypeInfo sourceType;

    /**
     * The type information of the target type in the conversion.
     */
    protected final TypeInfo targetType;

    /**
     * Constructs a new ConversionType with the specified source and target types.
     *
     * @param sourceType the source type of the conversion
     * @param targetType the target type of the conversion
     */
    public ConversionType(TypeInfo sourceType, TypeInfo targetType) {
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    /**
     * Copy constructor for ConversionType that creates a new ConversionType
     * based on the source and target types of an existing ConversionType instance.
     *
     * @param type the ConversionType to copy the source and target types from
     */
    public ConversionType(ConversionType type) {
        this.sourceType = type == null ? null : type.sourceType;
        this.targetType = type == null ? null : type.targetType;
    }

}
