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

package com.jd.live.agent.governance.openapi.headers;

import com.jd.live.agent.core.parser.json.JsonField;
import com.jd.live.agent.governance.openapi.examples.Example;
import com.jd.live.agent.governance.openapi.media.MediaType;
import com.jd.live.agent.governance.openapi.media.Schema;
import lombok.*;

import java.util.Map;

/**
 * Represents an OpenAPI Header object that describes a header sent with an API response.
 * <p>
 * Headers provide metadata about the response, such as pagination information,
 * rate limiting details, or other protocol-specific values.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Header {
    /**
     * Reference to a header defined elsewhere.
     * Uses the format "#/components/headers/{name}" in OpenAPI specification.
     */
    @JsonField("$ref")
    private String ref;

    /**
     * A brief description of the header.
     * CommonMark syntax may be used for rich text representation.
     */
    private String description;

    /**
     * Determines whether this header is mandatory.
     * If the header location is "path", this property is required and its value must be true.
     */
    private Boolean required;

    /**
     * Specifies that a header is deprecated and should be transitioned out of usage.
     */
    private Boolean deprecated;

    /**
     * When true, array values generate separate parameters for each array item.
     */
    private Boolean explode;

    /**
     * Describes how the header value will be serialized.
     * Valid values are "simple", "form", "matrix", "label", and "spaceDelimited".
     */
    private String style;

    /**
     * The schema defining the type used for the header.
     */
    private Schema schema;

    /**
     * Map of examples of the header value.
     * Each example should match the schema if provided.
     */
    private Map<String, Example> examples;

    /**
     * Example of the header value.
     * Should match the specified schema if present.
     */
    private Object example;

    /**
     * A map containing the representations for the header.
     * The key is the media type and the value describes it.
     */
    private Map<String, MediaType> content;

    /**
     * Custom specification extensions that start with "x-".
     */
    private Map<String, Object> extensions;
}