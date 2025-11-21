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
 * Allows configuration of the supported OAuth Flows.
 *
 * @see <a href="https://spec.openapis.org/oas/v3.0.3#oauth-flows-object">OpenAPI OAuth Flows Object</a>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthFlows {
    /**
     * Configuration for the OAuth Implicit flow
     */
    private OAuthFlow implicit;

    /**
     * Configuration for the OAuth Resource Owner Password flow
     */
    private OAuthFlow password;

    /**
     * Configuration for the OAuth Client Credentials flow
     */
    private OAuthFlow clientCredentials;

    /**
     * Configuration for the OAuth Authorization Code flow
     */
    private OAuthFlow authorizationCode;

    private Map<String, Object> extensions;
}