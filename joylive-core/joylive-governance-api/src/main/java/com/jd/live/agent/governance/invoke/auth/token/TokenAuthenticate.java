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
import com.jd.live.agent.governance.request.HttpRequest;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.governance.request.RpcRequest;
import com.jd.live.agent.governance.request.RpcRequest.RpcOutboundRequest;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Extension("token")
public class TokenAuthenticate implements Authenticate {

    private static final String KEY_TOKEN = "token";
    private static final String KEY_TOKEN_KEY = "token.key";

    @Override
    public AuthResult authenticate(ServiceRequest request, AuthPolicy policy) {
        String token = policy.getParameter(KEY_TOKEN);
        String key = policy.getParameter(KEY_TOKEN_KEY, KEY_AUTH);

        if (key != null && !key.isEmpty() && token != null && !token.isEmpty()) {
            return new AuthResult(token.equals(getToken(request, key)), "Token is not correct.");
        }
        return new AuthResult(true, null);
    }

    @Override
    public void inject(OutboundRequest request, AuthPolicy policy) {
        String token = policy.getParameter(KEY_TOKEN);
        String key = policy.getParameter(KEY_TOKEN_KEY, KEY_AUTH);

        if (key != null && !key.isEmpty() && token != null && !token.isEmpty()) {
            if (request.getHeader(key) == null) {
                if (request instanceof HttpOutboundRequest) {
                    token = decorate((HttpOutboundRequest) request, key, token);
                } else if (request instanceof RpcOutboundRequest) {
                    token = decorate((RpcOutboundRequest) request, key, token);
                }
                // add token by transmission
                RequestContext.getOrCreate().addCargo(key, token);
            }
        }
    }

    /**
     * Retrieves a token from the specified service request using the given key.
     *
     * @param request the service request
     * @param key     the key of the token
     * @return the token value, or null if not found
     */
    private String getToken(ServiceRequest request, String key) {
        if (request instanceof HttpRequest) {
            return getToken((HttpRequest) request, key);
        } else if (request instanceof RpcRequest) {
            return getToken((RpcRequest) request, key);
        }
        return null;
    }

    /**
     * Retrieves a token from the specified HTTP request using the given key.
     *
     * @param request the HTTP request
     * @param key     the key of the token
     * @return the token value, or null if not found
     */
    private String getToken(HttpRequest request, String key) {
        String requestToken = request.getHeader(key);
        if (key.equals(KEY_AUTH)) {
            if (requestToken != null && requestToken.startsWith(BASIC_PREFIX)) {
                requestToken = getBasicPassword(requestToken);
            }
        }
        return requestToken;
    }

    /**
     * Retrieves a token from the specified RPC request using the given key.
     *
     * @param request the RPC request
     * @param key     the key of the token
     * @return the token value, or null if not found
     */
    private String getToken(RpcRequest request, String key) {
        return (String) request.getAttachment(key);
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

    /**
     * Decorates a token before injecting it into the specified HTTP outbound request using the given key.
     *
     * @param request the HTTP outbound request
     * @param key     the key of the token
     * @param token   the value of the token
     * @return the decorated token value
     */
    private String decorate(HttpOutboundRequest request, String key, String token) {
        if (key.equals(KEY_AUTH)) {
            if (!token.startsWith(BASIC_PREFIX)) {
                token = new String(Base64.getEncoder().encode(token.getBytes(StandardCharsets.UTF_8)));
                token = BASIC_PREFIX + token;
            }
        }
        return token;
    }

    /**
     * Decorates a token before injecting it into the specified RPC outbound request using the given key.
     *
     * @param request the RPC outbound request
     * @param key     the key of the token
     * @param token   the value of the token
     * @return the decorated token value
     */
    private String decorate(RpcOutboundRequest request, String key, String token) {
        return token;
    }
}
