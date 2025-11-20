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

import com.jd.live.agent.governance.openapi.info.Info;
import com.jd.live.agent.governance.openapi.servers.Server;
import com.jd.live.agent.governance.openapi.tags.Tag;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * OpenAPI model root object (Swagger3 style).
 * Represents the full OpenAPI specification including info, paths, components, servers and security.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenApi {

    private String openapi;

    /**
     * API metadata information
     */
    private Info info;

    /**
     * API external documentation
     */
    private ExternalDoc externalDocs;

    /**
     * List of servers hosting the API
     */
    private List<Server> servers;

    /**
     * Security requirements for API
     */
    private List<Map<String, List<String>>> security;

    /**
     * Tags used for organizing operations
     */
    private List<Tag> tags;

    /**
     * Map of API paths and their operations
     */
    private Map<String, PathItem> paths;

    /**
     * Reusable components (schemas, parameters, responses, etc.)
     */
    private Components components;

    /**
     * The JSON Schema dialect used for schema objects within this specification
     */
    private String jsonSchemaDialect;

    /**
     * The version of the OpenAPI Specification used in this document
     */
    private String specVersion;

    /**
     * Map of webhooks that represent callbacks initiated by the API server to client-provided endpoints.
     * Unlike paths (where clients call the server), webhooks define how servers can call back to clients.
     */
    private Map<String, PathItem> webhooks;

    @Singular(ignoreNullCollections = true)
    private Map<String, Object> extensions;

}