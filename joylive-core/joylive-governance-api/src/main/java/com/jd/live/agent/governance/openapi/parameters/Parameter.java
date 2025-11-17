/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jd.live.agent.governance.openapi.parameters;

import com.jd.live.agent.governance.openapi.examples.Example;
import com.jd.live.agent.governance.openapi.media.MediaType;
import com.jd.live.agent.governance.openapi.media.Schema;
import lombok.*;

import java.util.Map;

/**
 * Represents a single operation parameter in OpenAPI specification.
 * <p>
 * A parameter defines how data is passed to or from an API operation.
 * Parameters can be passed in various ways, defined by the 'in' property.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parameter {
    /**
     * The name of the parameter. Required.
     */
    private String name;

    /**
     * The location of the parameter. Possible values are "query", "header", "path", or "cookie".
     * Required.
     */
    private String in;

    /**
     * A brief description of the parameter. CommonMark syntax may be used.
     */
    private String description;

    /**
     * Determines whether this parameter is mandatory.
     * If the parameter location is "path", this property is required and its value must be true.
     */
    private Boolean required;

    /**
     * Specifies that a parameter is deprecated and should be avoided.
     */
    private Boolean deprecated;

    /**
     * Sets the ability to pass empty-valued parameters.
     * This is valid only for query parameters and allows sending a parameter with an empty value.
     */
    private Boolean allowEmptyValue;

    /**
     * The schema defining the type used for the parameter.
     */
    private Schema schema;

    /**
     * A map containing the representations for the parameter.
     * The key is the media type and the value describes it.
     */
    private Map<String, MediaType> content;

    /**
     * Examples of the parameter's potential value. Each example should contain a value in the correct format.
     */
    private Map<String, Example> examples;

    /**
     * Example of the parameter's potential value.
     * The example object is mutually exclusive of the examples object.
     */
    private Object example;

    /**
     * Custom specification extensions that start with "x-".
     */
    private Map<String, Object> extensions;
}