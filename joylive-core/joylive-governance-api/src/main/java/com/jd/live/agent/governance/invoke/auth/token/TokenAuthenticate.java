/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.governance.invoke.auth.token;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.invoke.auth.Authenticate;
import com.jd.live.agent.governance.invoke.auth.Permission;
import com.jd.live.agent.governance.policy.service.auth.AuthPolicy;
import com.jd.live.agent.governance.policy.service.auth.TokenPolicy;
import com.jd.live.agent.governance.request.HttpRequest;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

@Extension("token")
public class TokenAuthenticate implements Authenticate {

    @Override
    public Permission authenticate(ServiceRequest request, AuthPolicy policy, String service, String consumer) {
        TokenPolicy tokenPolicy = policy.getTokenPolicy();
        if (tokenPolicy != null && tokenPolicy.isValid() && !decodeAndCompare(request, tokenPolicy)) {
            return Permission.failure("Token is not correct.");
        }
        return Permission.success();
    }

    @Override
    public void inject(OutboundRequest request, AuthPolicy policy, String service, String consumer) {
        TokenPolicy tokenPolicy = policy.getTokenPolicy();
        if (tokenPolicy != null && tokenPolicy.isValid()) {
            String key = tokenPolicy.getKey();
            if (request.getHeader(key) == null) {
                request.setHeader(key, encode(request, tokenPolicy));
            }
        }
    }

    /**
     * encode a token before injecting it into the specified HTTP outbound request.
     *
     * @param request the HTTP outbound request
     * @param policy  the token policy
     * @return the decorated token value
     */
    private String encode(OutboundRequest request, TokenPolicy policy) {
        String key = policy.getKey();
        String token = policy.getToken();
        if (request instanceof HttpOutboundRequest && key.equalsIgnoreCase(KEY_AUTH) && !token.startsWith(BASIC_PREFIX)) {
            token = BASIC_PREFIX + policy.getBase64Token();
        }
        return token;
    }

    /**
     * Retrieves a token from the specified service request using the given key.
     *
     * @param request the service request
     * @param policy  the token policy
     * @return the token value, or null if not found
     */
    private boolean decodeAndCompare(ServiceRequest request, TokenPolicy policy) {
        String key = policy.getKey();
        String token = request.getHeader(key);
        if (token != null
                && request instanceof HttpRequest
                && KEY_AUTH.equalsIgnoreCase(key)
                && token.startsWith(BASIC_PREFIX)) {
            // basic auth
            token = token.substring(BASIC_PREFIX.length());
            if (Objects.equals(policy.getBase64Token(), token)) {
                // base64 encoded,improve performance.
                return true;
            } else {
                try {
                    token = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
                    int pos = token.indexOf(":");
                    if (pos != -1) {
                        token = token.substring(pos + 1);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return Objects.equals(policy.getToken(), token);
    }
}
