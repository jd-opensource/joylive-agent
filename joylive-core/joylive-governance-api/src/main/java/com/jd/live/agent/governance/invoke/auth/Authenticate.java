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

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.policy.service.auth.AuthPolicy;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

/**
 * An interface for authenticating users based on a given policy.
 */
@Extensible("Authenticate")
public interface Authenticate {

    String KEY_AUTH = "Authorization";

    String BASIC_PREFIX = "Basic ";

    String BEARER_PREFIX = "Bearer ";

    /**
     * Authenticates a user based on the given policy and service request.
     *
     * @param request the service request to authenticate
     * @param policy  the authentication policy to use
     * @return the result of the authentication attempt
     */
    Permission authenticate(ServiceRequest request, AuthPolicy policy);

    /**
     * Injects the given authentication policy into the outbound request.
     *
     * @param request the service request to inject the policy into
     * @param policy  the authentication policy to inject
     */
    default void inject(OutboundRequest request, AuthPolicy policy) {

    }
}


