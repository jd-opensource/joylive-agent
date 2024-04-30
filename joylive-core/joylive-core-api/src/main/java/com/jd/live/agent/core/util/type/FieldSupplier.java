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
package com.jd.live.agent.core.util.type;

import com.jd.live.agent.core.util.type.generic.Generic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * An interface that provides methods to supply getter, setter, and generic methods for a given field.
 */
public interface FieldSupplier {

    /**
     * Retrieves the getter method associated with the specified field.
     *
     * @param field The field for which the getter method is to be obtained.
     * @return The getter {@link Method} for the specified field, or {@code null} if not found.
     */
    Method getGetter(Field field);

    /**
     * Retrieves the setter method associated with the specified field.
     *
     * @param field The field for which the setter method is to be obtained.
     * @return The setter {@link Method} for the specified field, or {@code null} if not found.
     */
    Method getSetter(Field field);

    /**
     * Retrieves the generic method associated with the specified field. This method could be used
     * for operations that are not strictly limited to getting or setting the field's value, but
     * might involve additional generic processing.
     *
     * @param field The field for which the generic method is to be obtained.
     * @return A generic related to the specified field, or {@code null} if not found.
     */
    Generic getGeneric(Field field);

}
