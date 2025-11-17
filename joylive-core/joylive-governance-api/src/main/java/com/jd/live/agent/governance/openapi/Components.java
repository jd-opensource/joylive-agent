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

package com.jd.live.agent.governance.openapi;

import com.jd.live.agent.governance.openapi.callbacks.Callback;
import com.jd.live.agent.governance.openapi.examples.Example;
import com.jd.live.agent.governance.openapi.headers.Header;
import com.jd.live.agent.governance.openapi.links.Link;
import com.jd.live.agent.governance.openapi.media.Schema;
import com.jd.live.agent.governance.openapi.parameters.Parameter;
import com.jd.live.agent.governance.openapi.parameters.RequestBody;
import com.jd.live.agent.governance.openapi.responses.ApiResponse;
import com.jd.live.agent.governance.openapi.security.SecurityScheme;
import lombok.*;

import java.util.Map;

/**
 * Represents the OpenAPI Components object that holds reusable elements for an API specification.
 * <p>
 * Components allow defining common objects that can be referenced throughout the API specification
 * using the $ref syntax, promoting reusability and reducing duplication.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Components {
    /**
     * The base reference path for schemas defined in the components section.
     */
    public static final String COMPONENTS_SCHEMAS_REF = "#/components/schemas/";

    /**
     * Map of reusable Schema objects keyed by schema name.
     * These define data types used across the API specification.
     */
    private Map<String, Schema> schemas;

    /**
     * Map of reusable Parameter objects keyed by parameter name.
     * These define common parameters that can be used across multiple operations.
     */
    private Map<String, Parameter> parameters;

    /**
     * Map of reusable RequestBody objects keyed by request body name.
     */
    private Map<String, RequestBody> requestBodies;

    /**
     * Map of reusable PathItem objects keyed by path item name.
     */
    private Map<String, PathItem> pathItems;

    /**
     * Map of reusable Header objects keyed by header name.
     * These define common response headers that can be referenced by responses.
     */
    private Map<String, Header> headers;

    /**
     * Map of reusable Response objects keyed by response name.
     * These define standardized API responses that can be referenced by operations.
     */
    private Map<String, ApiResponse> responses;

    /**
     * Map of reusable Callback objects keyed by callback name.
     */
    private Map<String, Callback> callbacks;

    /**
     * Map of reusable SecurityScheme objects keyed by security scheme name.
     * These define authentication and authorization mechanisms for the API.
     */
    private Map<String, SecurityScheme> securitySchemes;

    /**
     * Map of reusable Example objects keyed by example name.
     */
    private Map<String, Example> examples;

    /**
     * Map of reusable Link objects keyed by link name.
     */
    private Map<String, Link> links;

    /**
     * Custom specification extensions that start with "x-".
     */
    private Map<String, Object> extensions;
}