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

package com.jd.live.agent.core.openapi.spec.v3.security;

import lombok.*;

import java.util.Map;

/**
 * Represents an OAuth Flow configuration as defined by OpenAPI specification.
 *
 * @see <a href="https://spec.openapis.org/oas/v3.0.3#oauth-flow-object">OpenAPI OAuth Flow Object</a>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthFlow {
    /**
     * The authorization URL to be used for this flow
     */
    private String authorizationUrl;

    /**
     * The token URL to be used for this flow
     */
    private String tokenUrl;

    /**
     * The URL to be used for obtaining refresh tokens
     */
    private String refreshUrl;

    /**
     * The available scopes for the OAuth2 security scheme
     */
    private Map<String, String> scopes;

    private Map<String, Object> extensions;
}