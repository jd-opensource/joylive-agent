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
package com.jd.live.agent.core.util.type.generic;

import lombok.Getter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Represents generic information for parameters, return values, and exceptions.
 */
public class Generic {
    /**
     * The type of the field or parameter.
     */
    @Getter
    protected Type type;

    /**
     * The class representing the erasure of the type.
     */
    @Getter
    protected Class<?> erasure;

    /**
     * A collection of generic variables associated with this type.
     */
    protected volatile GenericVariableList variables = new GenericVariableZero();

    /**
     * Constructs a new Generic instance.
     *
     * @param type    The specific type of the field or parameter.
     * @param erasure The class representing the erasure of the type.
     */
    public Generic(Type type, Class<?> erasure) {
        this.type = type;
        this.erasure = erasure;
    }

    /**
     * Updates the recognized generic type.
     *
     * @param type The recognized generic type.
     */
    protected void setType(Type type) {
        this.type = type;
        if (erasure != type) {
            if (type instanceof Class) {
                erasure = (Class<?>) type;
            } else if (type instanceof ParameterizedType) {
                Type rawType = ((ParameterizedType) type).getRawType();
                if (rawType instanceof Class && rawType != erasure) {
                    erasure = (Class<?>) rawType;
                }
            }
        }
    }

    /**
     * Adds a variable to the collection.
     *
     * @param variable The variable to add.
     */
    protected void addVariable(final GenericVariable variable) {
        if (variable != null) {
            variables = variables.addVariable(variable);
        }
    }

    /**
     * Retrieves a variable by name.
     *
     * @param name The name of the variable.
     * @return The variable, if found.
     */
    public GenericVariable getVariable(final String name) {
        return variables.getVariable(name);
    }

    /**
     * Retrieves all generic variables.
     *
     * @return A list of generic variables.
     */
    public List<GenericVariable> getVariables() {
        return variables.getVariables();
    }

    /**
     * Places the variables in the specified parameters.
     *
     * @param parameters The parameters.
     */
    public void place(Map<String, Integer> parameters) {
        variables.place(parameters);
    }

    /**
     * Returns the size of the variables collection.
     *
     * @return The size.
     */
    public int size() {
        return variables.size();
    }
}

