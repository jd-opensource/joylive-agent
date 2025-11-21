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

package com.jd.live.agent.core.openapi.spec.v3;

import com.jd.live.agent.core.openapi.spec.v3.callbacks.Callback;
import com.jd.live.agent.core.openapi.spec.v3.examples.Example;
import com.jd.live.agent.core.openapi.spec.v3.headers.Header;
import com.jd.live.agent.core.openapi.spec.v3.links.Link;
import com.jd.live.agent.core.openapi.spec.v3.media.Schema;
import com.jd.live.agent.core.openapi.spec.v3.parameters.Parameter;
import com.jd.live.agent.core.openapi.spec.v3.parameters.RequestBody;
import com.jd.live.agent.core.openapi.spec.v3.responses.ApiResponse;
import com.jd.live.agent.core.openapi.spec.v3.security.SecurityScheme;
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
    public static final String COMPONENTS_PARAMETERS_REF = "#/components/parameters/";
    public static final String COMPONENTS_REQUEST_BODIES_REF = "#/components/requestBodies/";
    public static final String COMPONENTS_PATH_ITEMS_REF = "#/components/pathItems/";
    public static final String COMPONENTS_HEADERS_REF = "#/components/headers/";
    public static final String COMPONENTS_RESPONSES_REF = "#/components/responses/";
    public static final String COMPONENTS_CALLBACKS_REF = "#/components/callbacks/";
    public static final String COMPONENTS_SECURITY_SCHEMES_REF = "#/components/securitySchemes/";
    public static final String COMPONENTS_EXAMPLES_REF = "#/components/examples/";
    public static final String COMPONENTS_LINKS_REF = "#/components/links/";

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

    public Schema getSchema(String ref) {
        if (schemas == null || schemas.isEmpty()) {
            return null;
        }
        String name = getName(ref, COMPONENTS_SCHEMAS_REF);
        return name == null ? null : schemas.get(name);
    }

    public Schema getSchema(Schema schema) {
        if (schema == null) {
            return null;
        }
        return !isEmpty(schema.getRef()) ? getSchema(schema.getRef()) : schema;
    }

    public PathItem getPathItem(String ref) {
        if (pathItems == null || pathItems.isEmpty()) {
            return null;
        }
        String name = getName(ref, COMPONENTS_PATH_ITEMS_REF);
        return name == null ? null : pathItems.get(name);
    }

    public PathItem getPathItem(PathItem item) {
        if (item == null) {
            return null;
        }
        return !isEmpty(item.getRef()) ? getPathItem(item.getRef()) : item;
    }

    public Parameter getParameter(String ref) {
        if (parameters == null || parameters.isEmpty()) {
            return null;
        }
        String name = getName(ref, COMPONENTS_PARAMETERS_REF);
        return name == null ? null : parameters.get(name);
    }

    public Parameter getParameter(Parameter parameter) {
        if (parameter == null) {
            return null;
        }
        return !isEmpty(parameter.getRef()) ? getParameter(parameter.getRef()) : parameter;
    }

    public RequestBody getRequestBody(String ref) {
        if (requestBodies == null || requestBodies.isEmpty()) {
            return null;
        }
        String name = getName(ref, COMPONENTS_REQUEST_BODIES_REF);
        return name == null ? null : requestBodies.get(name);
    }

    public RequestBody getRequestBody(RequestBody body) {
        if (body == null) {
            return null;
        }
        return !isEmpty(body.getRef()) ? getRequestBody(body.getRef()) : body;
    }

    public ApiResponse getApiResponse(String ref) {
        if (responses == null || responses.isEmpty()) {
            return null;
        }
        String name = getName(ref, COMPONENTS_RESPONSES_REF);
        return name == null ? null : responses.get(name);
    }

    public ApiResponse getApiResponse(ApiResponse response) {
        if (response == null) {
            return null;
        }
        return !isEmpty(response.getRef()) ? getApiResponse(response.getRef()) : response;
    }

    private String getName(String ref, String prefix) {
        return ref == null || ref.length() <= prefix.length() ? null : ref.substring(prefix.length());
    }

    private boolean isEmpty(String ref) {
        return ref == null || ref.isEmpty();
    }

}