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
package com.jd.live.agent.core.parser;

import com.jd.live.agent.core.extension.annotation.Extensible;

import java.lang.reflect.Type;

/**
 * Interface for converting objects between different types.
 * Supports conversion to both Class and generic Type targets.
 */
@Extensible("ObjectConverter")
public interface ObjectConverter {

    String JACKSON = "jackson";

    /**
     * Converts source object to the specified class type.
     *
     * @param source the source object to convert
     * @param type   the target class type
     * @return the converted object
     */
    <T> T convert(Object source, Class<T> type);

    /**
     * Converts source object to the specified generic type.
     *
     * @param source the source object to convert
     * @param type   the target generic type
     * @return the converted object
     */
    Object convert(Object source, Type type);

}
