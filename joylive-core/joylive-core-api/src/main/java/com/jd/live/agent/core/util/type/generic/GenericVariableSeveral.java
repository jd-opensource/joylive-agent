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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericVariableSeveral implements GenericVariableList {

    private final Map<String, GenericVariable> map = new HashMap<>();

    private final List<GenericVariable> variables = new ArrayList<>(4);

    public GenericVariableSeveral(GenericVariable... vars) {
        if (vars != null) {
            for (GenericVariable var : vars) {
                addVariable(var);
            }
        }
    }

    @Override
    public GenericVariableList addVariable(GenericVariable variable) {
        if (variable != null) {
            if (map.putIfAbsent(variable.getName(), variable) == null)
                variables.add(variable);
        }
        return this;
    }

    @Override
    public GenericVariable getVariable(String name) {
        return name == null ? null : map.get(name);
    }

    @Override
    public List<GenericVariable> getVariables() {
        return variables;
    }

    @Override
    public void place(Map<String, Integer> parameters) {
        if (parameters != null) {
            Integer pos;
            for (GenericVariable variable : variables) {
                pos = parameters.get(variable.getName());
                if (pos != null)
                    variable.setParameter(pos);
            }
        }
    }

    @Override
    public int size() {
        return variables.size();
    }
}
