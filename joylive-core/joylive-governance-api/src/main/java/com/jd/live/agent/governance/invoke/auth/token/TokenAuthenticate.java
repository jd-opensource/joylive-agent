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
import com.jd.live.agent.governance.invoke.auth.AbstractAuthenticate;
import com.jd.live.agent.governance.invoke.auth.Permission;
import com.jd.live.agent.governance.policy.service.auth.AuthPolicy;
import com.jd.live.agent.governance.policy.service.auth.TokenPolicy;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.jd.live.agent.governance.policy.service.auth.AuthPolicy.AUTH_TYPE_TOKEN;

@Extension(AUTH_TYPE_TOKEN)
public class TokenAuthenticate extends AbstractAuthenticate {

    @Override
    public Permission authenticate(ServiceRequest request, AuthPolicy policy, String service, String consumer) {
        return authenticate(policy.getTokenPolicies(), p -> authenticate(request, p));
    }

    @Override
    public void inject(OutboundRequest request, AuthPolicy policy, String service, String consumer) {
        TokenPolicy tokenPolicy = policy.getLatestEffectiveTokenPolicy();
        if (tokenPolicy == null) {
            return;
        }
        String key = tokenPolicy.getKeyOrDefault(KEY_AUTH);
        if (request.getHeader(key) != null) {
            return;
        }
        String token = decorate(request, tokenPolicy, key, tokenPolicy.getToken(), () -> tokenPolicy.getBase64Token());
        request.setHeader(key, token);
    }

    /**
     * Authenticates a service request against a token policy.
     * Supports both Basic Auth and simple token authentication.
     *
     * @param request the service request to authenticate
     * @param policy  the token policy to validate against
     * @return success permission if authenticated, failure permission otherwise
     */
    private Permission authenticate(ServiceRequest request, TokenPolicy policy) {
        Token token = getToken(request, policy);
        if (token == null || !token.isValid()) {
            return Permission.failure("Missing token");
        }
        String value = token.getToken();
        if (token.isAuthorization()) {
            if (value.equals(policy.getBase64Token())) {
                // base64 encoded,improve performance.
                return Permission.success();
            } else if (value.length() > policy.getToken().length()) {
                // base64 will add more chars
                try {
                    value = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
                    int pos = value.indexOf(":");
                    if (pos != -1) {
                        value = value.substring(pos + 1);
                    }
                } catch (Exception e) {
                    return Permission.failure("Invalid token encoding");
                }
            }
        }
        return value.equals(policy.getToken()) ? Permission.success() : Permission.failure("Token is not correct.");
    }

}
