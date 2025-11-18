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

import com.jd.live.agent.governance.openapi.headers.Header;
import lombok.*;

import java.util.Map;

/**
 * Represents a single encoding definition applied to a single schema property.
 *
 * @see <a href="https://spec.openapis.org/oas/v3.0.3#encoding-object">OpenAPI Encoding Object</a>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Encoding {
    /**
     * The Content-Type for encoding specific property
     */
    private String contentType;

    /**
     * A map of headers for the encoding
     */
    private Map<String, Header> headers;

    /**
     * Describes how the parameter value will be serialized
     */
    private String style;

    /**
     * Whether the parameter value should be exploded
     */
    private Boolean explode;

    /**
     * Whether reserved characters in parameter values are allowed
     */
    private Boolean allowReserved;

    private Map<String, Object> extensions;

}