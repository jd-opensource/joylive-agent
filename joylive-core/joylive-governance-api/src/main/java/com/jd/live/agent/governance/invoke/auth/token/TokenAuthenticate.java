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

@Extension("token")
public class TokenAuthenticate implements Authenticate {

    private static final String TOKEN = "token";
    private static final String X_SERVICE_TOKEN = "x-service-token";
    private static final String TOKEN_KEY = "token.key";

    @Override
    public AuthResult authenticate(ServiceRequest request, AuthPolicy policy) {
        String token = policy.getParameter(TOKEN);
        String tokenKey = policy.getParameter(TOKEN_KEY, X_SERVICE_TOKEN);

        if (token != null && !token.isEmpty()) {
            String requestToken = null;
            if (tokenKey != null) {
                if (request instanceof HttpRequest) {
                    requestToken = ((HttpRequest) request).getHeader(tokenKey);
                } else if (request instanceof RpcRequest) {
                    requestToken = (String) ((RpcRequest) request).getAttachment(tokenKey);
                }
            }
            return new AuthResult(token.equals(requestToken), "Token is not correct.");
        }
        return new AuthResult(true, null);
    }
}
