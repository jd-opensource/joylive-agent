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
package com.jd.live.agent.governance.invoke.auth;

import com.jd.live.agent.governance.policy.service.auth.AuthStrategy;
import com.jd.live.agent.governance.request.HttpRequest;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import lombok.Getter;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractAuthenticate implements Authenticate {

    /**
     * Authenticate using multiple policies.
     *
     * @param <T>           type of authentication strategy
     * @param policies      list of authentication policies to try
     * @param authenticator function to authenticate each policy
     * @return permission result, success if no valid policies found
     */
    protected <T extends AuthStrategy> Permission authenticate(List<T> policies, Function<T, Permission> authenticator) {
        if (policies != null && !policies.isEmpty()) {
            long now = System.currentTimeMillis();
            int counter = 0;
            Permission permission = null;
            for (T tokenPolicy : policies) {
                if (tokenPolicy.isEffective(now)) {
                    counter++;
                    permission = authenticator.apply(tokenPolicy);
                    if (permission.isSuccess()) {
                        return permission;
                    }
                }
            }
            if (counter > 0) {
                return permission;
            }
        }
        return Permission.success();
    }

    /**
     * Extracts token from request header. For Authorization header like "Bearer xxx" or "Basic xxx",
     * it returns the token part with authorization=true. For other headers, returns raw value with authorization=false.
     *
     * @param request the service request
     * @param policy  the auth policy
     * @return token object, or null if not found
     */
    protected Token getToken(ServiceRequest request, AuthStrategy policy) {
        String key = policy.getKeyOrDefault(KEY_AUTH);
        String token = request.getHeader(key);

        if (token != null) {
            if (request instanceof HttpRequest && KEY_AUTH.equalsIgnoreCase(key)) {
                String scheme = policy.getAuthScheme();
                if (scheme != null && !scheme.isEmpty()) {
                    int length = scheme.length();
                    // Authorization:Bearer xxxx
                    // Authorization:Basic xxxx
                    if (token.startsWith(scheme) && token.length() > length + 1 && token.charAt(length) == ' ') {
                        // Bearer/Basic auth
                        return new Token(token.substring(length + 1), true);
                    }

                }
            }
            return new Token(token, false);
        }
        return null;
    }

    /**
     * Decorates token with auth scheme for HTTP Authorization header if needed.
     *
     * @param request HTTP outbound request
     * @param policy  auth strategy
     * @param key     header key
     * @param token   original token
     * @param base64  base64 token supplier
     * @return decorated token value
     */
    protected String decorate(OutboundRequest request, AuthStrategy policy, String key, String token, Supplier<String> base64) {
        if (request instanceof HttpOutboundRequest
                && key.equalsIgnoreCase(KEY_AUTH)
                && !token.startsWith(policy.getAuthScheme())) {
            token = policy.getAuthScheme() + " " + (base64 == null ? token : base64.get());
        }
        return token;
    }

    /**
     * Represents a token value with its authorization type.
     */
    @Getter
    protected static class Token {

        /**
         * The token value
         */
        protected final String token;

        /**
         * Whether this is an Authorization header token (Bearer/Basic)
         */
        protected final boolean authorization;

        public Token(String token, boolean authorization) {
            this.token = token;
            this.authorization = authorization;
        }

        public boolean isValid() {
            return token != null && !token.isEmpty();
        }
    }
}
