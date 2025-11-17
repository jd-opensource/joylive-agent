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
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Categorizes API parameters by their type for simplified processing.
 */
@Getter
public class ParameterComponents {

    /**
     * Standard parameters (excluding body and header types)
     */
    private Map<String, Parameter> parameters;

    /**
     * Request body parameters
     */
    private Map<String, BodyParameter> bodies;

    /**
     * HTTP header parameters
     */
    private Map<String, HeaderParameter> headers;

    /**
     * Constructs component container and sorts parameters by type
     *
     * @param parameters Raw parameter map to categorize
     */
    public ParameterComponents(Map<String, Parameter> parameters) {
        Map<String, Parameter> params = null;
        Map<String, BodyParameter> bodies = null;
        Map<String, HeaderParameter> headers = null;
        if (parameters != null && !parameters.isEmpty()) {
            params = new LinkedHashMap<>(parameters.size());
            bodies = new LinkedHashMap<>();
            headers = new LinkedHashMap<>();
            for (Map.Entry<String, Parameter> entry : parameters.entrySet()) {
                Parameter parameter = entry.getValue();
                if (parameter instanceof BodyParameter) {
                    bodies.put(entry.getKey(), (BodyParameter) parameter);
                } else if (parameter instanceof HeaderParameter) {
                    headers.put(entry.getKey(), (HeaderParameter) parameter);
                } else {
                    params.put(entry.getKey(), parameter);
                }
            }
        }
        this.parameters = params;
        this.bodies = bodies;
        this.headers = headers;
    }
}
