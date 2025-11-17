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

package com.jd.live.agent.governance.openapi.media;

import com.jd.live.agent.governance.openapi.examples.Example;
import lombok.*;

import java.util.Map;

/**
 * Represents an OpenAPI Media Type object that describes the format and structure of a request or response body.
 * <p>
 * Media Type objects provide schema information for specific content types (like application/json,
 * application/xml, etc.) and can include examples of the content.
 *
 * @see <a href="https://spec.openapis.org/oas/v3.0.3#media-type-object">OpenAPI Media Type Object</a>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaType {
    /**
     * The schema defining the structure and validation constraints of the content.
     * Describes the data format for this media type.
     */
    private Schema schema;

    /**
     * A map between property names and their encoding information.
     * Applies to multipart and application/x-www-form-urlencoded request bodies.
     */
    private Map<String, Encoding> encoding;

    /**
     * Map of examples keyed by a unique name.
     * Each example should match the media type and specified schema if present.
     */
    private Map<String, Example> examples;

    /**
     * Example of the media type.
     * The example object should be compatible with the specified schema.
     * Either example or examples can be provided, but not both.
     */
    private Object example;

    /**
     * Flag indicating whether an example has been explicitly set.
     */
    private boolean exampleSetFlag;

    /**
     * Custom specification extensions that start with "x-".
     */
    private Map<String, Object> extensions;
}