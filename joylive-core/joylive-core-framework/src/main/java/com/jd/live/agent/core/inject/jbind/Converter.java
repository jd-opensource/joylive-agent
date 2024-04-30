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

import com.jd.live.agent.core.exception.ConvertException;
import com.jd.live.agent.core.extension.annotation.Extensible;

/**
 * The Converter interface defines a contract for converting an object from one type
 * to another. Implementations of this interface are responsible for the actual
 * conversion logic.
 */
public interface Converter {

    /**
     * A constant representing a no-operation converter that simply returns the source
     * object without any conversion.
     */
    Converter NONE = Conversion::getSource;

    /**
     * A constant representing an error converter that throws a ConvertException when
     * the convert method is called.
     */
    Converter ERROR = c -> {
        throw new ConvertException("failed to convert between " + c.getSourceType().getRawType() + " and " + c.getTargetType().getRawType() + ". path=" + c.getPath());
    };

    /**
     * Converts the given Conversion object to an instance of the target type.
     *
     * @param conversion the Conversion object that contains the source object,
     *                   source type, target type, and context for the conversion
     * @return the converted object instance
     * @throws Exception if an error occurs during the conversion process
     */
    Object convert(Conversion conversion) throws Exception;

    /**
     * The FundamentalConverter interface extends Converter and provides access to
     * the source and target types involved in the conversion operation.
     */
    @Extensible("FundamentalConverter")
    interface FundamentalConverter extends Converter {

        /**
         * Returns the source type of the conversion operation.
         *
         * @return the source type
         */
        Class<?> getSourceType();

        /**
         * Returns the target type of the conversion operation.
         *
         * @return the target type
         */
        Class<?> getTargetType();

    }
}

