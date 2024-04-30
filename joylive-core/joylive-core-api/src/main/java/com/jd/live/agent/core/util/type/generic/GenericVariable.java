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

import java.lang.reflect.Type;

/**
 * Represents a generic variable with a name, type, and an optional parameter.
 */
@Getter
public class GenericVariable {

    private final String name;

    private Type type;

    private int parameter = -1;

    /**
     * Constructs a new GenericVariable with the specified name.
     *
     * @param name The name of the variable.
     */
    public GenericVariable(String name) {
        this.name = name;
    }

    /**
     * Constructs a new GenericVariable with the specified name and type.
     *
     * @param name The name of the variable.
     * @param type The type of the variable.
     */
    public GenericVariable(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Sets the parameter of this variable. This method is protected and can only be called within the class itself
     * or by classes that extend this class.
     *
     * @param parameter The parameter to set.
     */
    protected void setParameter(int parameter) {
        this.parameter = parameter;
    }
}