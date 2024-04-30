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

import java.util.List;
import java.util.Map;

/**
 * Defines an interface for managing a list of generic variables. This interface provides methods for adding variables,
 * retrieving a specific variable by name, getting a list of all variables, placing values into variables based on a map
 * of parameters, and getting the size of the variable list.
 */
public interface GenericVariableList {

    /**
     * Adds a new variable to the list.
     *
     * @param variable The {@link GenericVariable} to be added to the list.
     * @return The {@code GenericVariableList} instance, allowing for method chaining.
     */
    GenericVariableList addVariable(GenericVariable variable);

    /**
     * Retrieves a variable by its name.
     *
     * @param name The name of the variable to be retrieved.
     * @return The {@link GenericVariable} with the specified name, or {@code null} if no such variable exists in the list.
     */
    GenericVariable getVariable(String name);

    /**
     * Returns a list of all variables in the variable list.
     *
     * @return A {@link List} containing all the {@link GenericVariable} instances in the list.
     */
    List<GenericVariable> getVariables();

    /**
     * Places values into the variables based on the provided map of parameters. The map's keys represent variable names,
     * and the values represent the values to be placed into those variables.
     *
     * @param parameters A {@link Map} containing variable names and their corresponding values to be placed.
     */
    void place(Map<String, Integer> parameters);

    /**
     * Returns the size of the variable list, indicating how many variables are currently managed by this list.
     *
     * @return The number of variables in the list.
     */
    int size();

}