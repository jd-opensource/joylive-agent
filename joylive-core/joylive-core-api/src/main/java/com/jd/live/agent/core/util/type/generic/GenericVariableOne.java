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
import java.util.List;
import java.util.Map;

public class GenericVariableOne implements GenericVariableList {

    protected GenericVariable variable;

    public GenericVariableOne(GenericVariable variable) {
        this.variable = variable;
    }

    @Override
    public GenericVariableList addVariable(GenericVariable variable) {
        return variable == null ? this : new GenericVariableSeveral(this.variable, variable);
    }

    @Override
    public GenericVariable getVariable(String name) {
        return name == null || !name.equals(variable.getName()) ? null : variable;
    }

    @Override
    public List<GenericVariable> getVariables() {
        List<GenericVariable> result = new ArrayList<>(1);
        result.add(variable);
        return result;
    }

    @Override
    public void place(Map<String, Integer> parameters) {
        if (parameters != null) {
            Integer pos = parameters.get(variable.getName());
            if (pos != null)
                variable.setParameter(pos);
        }
    }

    @Override
    public int size() {
        return 1;
    }
}
