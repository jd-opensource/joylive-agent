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
package com.jd.live.agent.plugin.application.springboot.v2.openapi.v2;

import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.RefParameter;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Container class for categorizing Swagger parameters into reference, body, and other parameters.
 */
@Getter
public class ParameterList {

    /**
     * Reference parameter if present in the parameter list.
     */
    private RefParameter ref;

    /**
     * Body parameter if present in the parameter list.
     */
    private BodyParameter body;

    /**
     * List of parameters that are neither reference nor body parameters.
     */
    private List<io.swagger.models.parameters.Parameter> parameters;

    /**
     * Constructs a Parameters object by categorizing the provided parameter list.
     *
     * @param parameters List of Swagger parameters to categorize
     */
    public ParameterList(List<Parameter> parameters) {
        RefParameter refParameter = null;
        BodyParameter bodyParameter = null;
        List<io.swagger.models.parameters.Parameter> others = null;
        if (parameters != null && !parameters.isEmpty()) {
            others = new ArrayList<>(parameters.size());
            for (io.swagger.models.parameters.Parameter parameter : parameters) {
                if (parameter instanceof BodyParameter) {
                    bodyParameter = (BodyParameter) parameter;
                } else if (parameter instanceof RefParameter) {
                    refParameter = (RefParameter) parameter;
                } else {
                    others.add(parameter);
                }
            }
        }
        this.ref = refParameter;
        this.body = bodyParameter;
        this.parameters = others;
    }
}
