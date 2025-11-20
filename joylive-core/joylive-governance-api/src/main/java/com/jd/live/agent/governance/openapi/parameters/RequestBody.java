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

import com.jd.live.agent.core.parser.json.JsonField;
import com.jd.live.agent.governance.openapi.media.MediaType;
import lombok.*;

import java.util.Map;

/**
 * Represents a request body in OpenAPI specification.
 * <p>
 * The request body describes data sent to an API when making requests
 * that include content in the message body (typically with POST, PUT, and PATCH methods).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestBody {

    /**
     * Reference to a request body defined elsewhere.
     */
    @JsonField("$ref")
    private String ref;

    private String name;

    /**
     * A brief description of the request body. CommonMark syntax may be used.
     */
    private String description;

    /**
     * The content of the request body. Key is a media type (e.g., application/json),
     * and the value describes the content for that media type.
     */
    private Map<String, MediaType> content;

    /**
     * Determines if the request body is required in the request.
     * Defaults to false.
     */
    private Boolean required;

    /**
     * Custom specification extensions that start with "x-".
     */
    private Map<String, Object> extensions;

    public String getName() {
        return name == null || name.isEmpty() ? "body" : name;
    }

    public boolean required() {
        return this.required != null && this.required;
    }
}