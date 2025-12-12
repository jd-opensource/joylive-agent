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
package com.jd.live.agent.core.openapi.spec.v3.links;

import com.jd.live.agent.core.parser.annotation.JsonField;
import com.jd.live.agent.core.openapi.spec.v3.headers.Header;
import com.jd.live.agent.core.openapi.spec.v3.servers.Server;
import lombok.*;

import java.util.Map;

/**
 * Represents an OpenAPI Link object that describes a possible design-time link between operations.
 * <p>
 * Links enable the client to navigate from one API operation to another based on the response.
 * They provide a way to describe relationships between operations and can be used to drive
 * hypermedia-based client navigation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Link {
    /**
     * Reference to a link defined elsewhere.
     * Uses the format "#/components/links/{name}" in OpenAPI specification.
     */
    @JsonField("$ref")
    private String ref;

    /**
     * A description of the link.
     * CommonMark syntax may be used for rich text representation.
     */
    private String description;

    /**
     * A relative or absolute URI reference to an OAS operation.
     * This field is mutually exclusive with operationId.
     */
    private String operationRef;

    /**
     * The name of an existing, resolvable OAS operation, as defined with a unique operationId.
     * This field is mutually exclusive with operationRef.
     */
    private String operationId;

    /**
     * A map representing parameters to pass to the operation as specified with operationId or operationRef.
     * The key is the parameter name to be used, and the value is an expression that can reference values
     * from the source operation.
     */
    private Map<String, String> parameters;

    /**
     * A literal value or expression to use as a request body when calling the target operation.
     */
    private Object requestBody;

    /**
     * Map of headers to be sent with the link request.
     *
     * @deprecated This field has been deprecated in the OpenAPI specification.
     */
    @Deprecated
    private Map<String, Header> headers;

    /**
     * Server object to be used by the target operation.
     */
    private Server server;

    /**
     * Custom specification extensions that start with "x-".
     */
    private Map<String, Object> extensions;
}