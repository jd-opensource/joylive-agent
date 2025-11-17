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
import com.jd.live.agent.governance.openapi.parameters.Parameter;
import com.jd.live.agent.governance.openapi.parameters.RequestBody;
import com.jd.live.agent.governance.openapi.responses.ApiResponse;
import com.jd.live.agent.governance.openapi.security.SecurityRequirement;
import com.jd.live.agent.governance.openapi.servers.Server;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Represents an OpenAPI Operation object that describes a single API operation on a path.
 * <p>
 * An Operation is the primary building block of the OpenAPI specification, defining
 * the HTTP method, parameters, request body, responses, and other details needed to
 * interact with an API endpoint.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Operation {

    /**
     * A short summary of what the operation does.
     */
    private String summary;

    /**
     * A verbose explanation of the operation behavior.
     * CommonMark syntax may be used for rich text representation.
     */
    private String description;

    /**
     * Unique string used to identify the operation.
     * The id MUST be unique among all operations described in the API.
     */
    private String operationId;

    /**
     * A list of tags for API documentation control.
     * Tags can be used for logical grouping of operations.
     */
    private List<String> tags;

    /**
     * A list of parameters that are applicable for this operation.
     * Parameters defined here will override any parameters defined at the path level.
     */
    private List<Parameter> parameters;

    /**
     * The request body applicable for this operation.
     */
    private RequestBody requestBody;

    /**
     * The list of possible responses as they are returned from executing this operation.
     * Key is the status code, value is the response definition.
     */
    private Map<String, ApiResponse> responses;

    /**
     * Map of possible out-of-band callbacks related to the parent operation.
     * Each value in the map is a Callback Object that describes a request that
     * may be initiated by the API provider and the expected responses.
     */
    private Map<String, Callback> callbacks;

    /**
     * A declaration of which security mechanisms can be used for this operation.
     * The list of values includes alternative security requirement objects that can be used.
     */
    private List<SecurityRequirement> security;

    /**
     * Declares this operation to be deprecated. Consumers SHOULD refrain from usage.
     */
    private Boolean deprecated;

    /**
     * Additional external documentation for this operation.
     */
    private ExternalDocumentation externalDocs;

    /**
     * An alternative server array to service this operation.
     * If provided, it overrides the default server array at the root level.
     */
    private List<Server> servers = null;

    /**
     * Custom specification extensions that start with "x-".
     */
    private Map<String, Object> extensions;
}