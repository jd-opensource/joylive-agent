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

package com.jd.live.agent.core.openapi.spec.v3.examples;

import com.jd.live.agent.core.parser.annotation.JsonField;
import lombok.*;

import java.util.Map;

/**
 * Represents an example object in OpenAPI specification.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Example {

    /**
     * Reference to an example defined elsewhere.
     */
    @JsonField("$ref")
    private String ref;

    /**
     * Short summary of the example.
     */
    private String summary;

    /**
     * Long description of the example. CommonMark syntax can be used.
     */
    private String description;

    /**
     * Embedded literal example value.
     */
    private Object value;

    /**
     * URL that points to the literal example.
     */
    private String externalValue;

    /**
     * Flag indicating whether a value has been explicitly set.
     */
    private boolean valueSetFlag;

    /**
     * Custom specification extensions that start with "x-".
     */
    private Map<String, Object> extensions;

}