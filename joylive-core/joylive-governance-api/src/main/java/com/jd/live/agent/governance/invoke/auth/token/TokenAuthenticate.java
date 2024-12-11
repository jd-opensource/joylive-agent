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
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.invoke.auth.AuthResult;
import com.jd.live.agent.governance.invoke.auth.Authenticate;
import com.jd.live.agent.governance.policy.service.auth.AuthPolicy;
import com.jd.live.agent.governance.policy.service.auth.TokenPolicy;
import com.jd.live.agent.governance.request.HttpRequest;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Extension("token")
public class TokenAuthenticate implements Authenticate {

    @Override
    public AuthResult authenticate(ServiceRequest request, AuthPolicy policy) {
        TokenPolicy tokenPolicy = policy.getTokenPolicy();
        if (tokenPolicy != null && tokenPolicy.isValid()) {
            String key = tokenPolicy.getKey();
            String token = tokenPolicy.getToken();
            return new AuthResult(token.equals(decode(request, key)), "Token is not correct.");
        }
        return new AuthResult(true, null);
    }

    @Override
    public void inject(OutboundRequest request, AuthPolicy policy) {
        TokenPolicy tokenPolicy = policy.getTokenPolicy();
        if (tokenPolicy != null && tokenPolicy.isValid()) {
            String key = tokenPolicy.getKey();
            String token = tokenPolicy.getToken();
            if (request.getHeader(key) == null) {
                token = encode(request, key, token);
                // TODO request.setHeader(key, token);
                // add token by transmission
                RequestContext.getOrCreate().addCargo(key, token);
            }
        }
    }

    /**
     * encode a token before injecting it into the specified HTTP outbound request using the given key.
     *
     * @param request the HTTP outbound request
     * @param key     the key of the token
     * @param token   the value of the token
     * @return the decorated token value
     */
    protected String encode(OutboundRequest request, String key, String token) {
        if (request instanceof HttpOutboundRequest && key.equalsIgnoreCase(KEY_AUTH) && !token.startsWith(BASIC_PREFIX)) {
            token = new String(Base64.getEncoder().encode(token.getBytes(StandardCharsets.UTF_8)));
            token = BASIC_PREFIX + token;
        }
        return token;
    }

    /**
     * Retrieves a token from the specified service request using the given key.
     *
     * @param request the service request
     * @param key     the key of the token
     * @return the token value, or null if not found
     */
    private String decode(ServiceRequest request, String key) {
        String token = request.getHeader(key);
        if (token != null
                && request instanceof HttpRequest
                && KEY_AUTH.equalsIgnoreCase(key)
                && token.startsWith(BASIC_PREFIX)) {
            token = getBasicPassword(token);
        }
        return token;
    }

    /**
     * Extracts the password from a basic authentication token.
     *
     * @param token the basic authentication token
     * @return the extracted password
     */
    private String getBasicPassword(String token) {
        token = token.substring(BASIC_PREFIX.length());
        try {
            token = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            int pos = token.indexOf(":");
            if (pos != -1) {
                token = token.substring(pos + 1);
            }
        } catch (Exception ignored) {
        }
        return token;
    }

}
