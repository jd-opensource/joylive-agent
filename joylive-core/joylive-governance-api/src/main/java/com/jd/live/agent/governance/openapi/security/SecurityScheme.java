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

package com.jd.live.agent.governance.openapi.security;

import com.jd.live.agent.core.parser.json.JsonField;
import lombok.*;

import java.util.Map;

/**
 * Defines a security scheme that can be used by the operations.
 *
 * @see <a href="https://spec.openapis.org/oas/v3.0.3#security-scheme-object">OpenAPI Security Scheme Object</a>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityScheme {

    /**
     * Reference to a security scheme defined in components section
     */
    @JsonField("$ref")
    private String ref;

    /**
     * A short description for security scheme
     */
    private String description;

    /**
     * The type of the security scheme (apiKey, http, oauth2, openIdConnect)
     */
    private String type;

    /**
     * The name of the header, query or cookie parameter to be used
     */
    private String name;

    /**
     * The location of the API key (query, header or cookie)
     */
    private String in;

    /**
     * The name of the HTTP Authorization scheme
     */
    private String scheme;

    /**
     * A hint to the client to identify how the bearer token is formatted
     */
    private String bearerFormat;

    /**
     * OAuth flow configurations
     */
    private OAuthFlows flows;

    /**
     * OpenID Connect URL to discover OAuth2 configuration values
     */
    private String openIdConnectUrl;

    private Map<String, Object> extensions;
}