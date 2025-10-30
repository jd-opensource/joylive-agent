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

    /**
     * Authenticates a request using given policy.
     *
     * @param request Service request to authenticate
     * @param policy Authentication policy to apply
     * @param service Target service name
     * @param consumer Requesting consumer identity
     * @return Authentication permission result
     */
    Permission authenticate(ServiceRequest request, AuthPolicy policy, String service, String consumer);

    /**
     * Applies authentication policy to outbound request.
     *
     * @param request Outbound request to modify
     * @param policy Authentication policy to inject
     * @param service Target service name
     * @param consumer Requesting consumer identity
     */
    default void inject(OutboundRequest request, AuthPolicy policy, String service, String consumer) {
    }
}


