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

package com.jd.live.agent.core.openapi.spec.v3.responses;

import com.jd.live.agent.core.parser.annotation.JsonField;
import com.jd.live.agent.core.openapi.spec.v3.headers.Header;
import com.jd.live.agent.core.openapi.spec.v3.links.Link;
import com.jd.live.agent.core.openapi.spec.v3.media.MediaType;
import lombok.*;

import java.util.Map;

/**
 * Represents an OpenAPI Response object that describes a single response from an API operation.
 * <p>
 * A Response object defines the expected response data for a specific HTTP status code.
 * It includes details about the response payload, headers, and possible links to other operations.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse {

    /**
     * Reference to a response defined elsewhere.
     * Uses the format "#/components/responses/{name}" in OpenAPI specification.
     */
    @JsonField("$ref")
    private String ref;

    /**
     * A description of the response.
     * Required unless the response is a reference.
     * CommonMark syntax may be used for rich text representation.
     */
    private String description;

    /**
     * A map containing descriptions of potential response payloads.
     * The key is a media type or media type range and the value describes it.
     * For responses that match multiple keys, the most specific key is used.
     */
    private Map<String, MediaType> content;

    /**
     * Maps a header name to its definition.
     * Headers that are common to all responses can be specified at the operation level.
     */
    private Map<String, Header> headers;

    /**
     * A map of operations links that can be followed from the response.
     * The key of the map is a short name for the link, and the value is the Link object.
     */
    private Map<String, Link> links;

    /**
     * Custom specification extensions that start with "x-".
     */
    private Map<String, Object> extensions;
}