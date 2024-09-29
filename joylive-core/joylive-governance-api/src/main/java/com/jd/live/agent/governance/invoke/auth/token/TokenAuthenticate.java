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
import com.jd.live.agent.governance.invoke.auth.AuthResult;
import com.jd.live.agent.governance.invoke.auth.Authenticate;
import com.jd.live.agent.governance.policy.service.auth.AuthPolicy;
import com.jd.live.agent.governance.request.HttpRequest;
import com.jd.live.agent.governance.request.RpcRequest;
import com.jd.live.agent.governance.request.ServiceRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Extension("token")
public class TokenAuthenticate implements Authenticate {

    private static final String KEY_TOKEN = "token";
    private static final String KEY_TOKEN_KEY = "token.key";

    @Override
    public AuthResult authenticate(ServiceRequest request, AuthPolicy policy) {
        String token = policy.getParameter(KEY_TOKEN);
        String tokenKey = policy.getParameter(KEY_TOKEN_KEY, KEY_AUTH);

        if (token != null && !token.isEmpty()) {
            String requestToken = null;
            if (tokenKey != null) {
                if (request instanceof HttpRequest) {
                    requestToken = ((HttpRequest) request).getHeader(tokenKey);
                    if (tokenKey.equals(KEY_AUTH)) {
                        if (requestToken.startsWith(BASIC_PREFIX)) {
                            requestToken = getBasicPassword(requestToken);
                        }
                    }
                } else if (request instanceof RpcRequest) {
                    requestToken = (String) ((RpcRequest) request).getAttachment(tokenKey);
                }
            }
            return new AuthResult(token.equals(requestToken), "Token is not correct.");
        }
        return new AuthResult(true, null);
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
