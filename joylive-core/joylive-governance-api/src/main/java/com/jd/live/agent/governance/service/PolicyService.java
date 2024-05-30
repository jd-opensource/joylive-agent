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
package com.jd.live.agent.governance.service;

import com.jd.live.agent.core.service.AgentService;
import com.jd.live.agent.governance.policy.PolicyType;

/**
 * Represents a service that deals with policies, extending the functionalities
 * of an {@link AgentService}. This interface provides additional capabilities
 * specifically related to policy management.
 */
public interface PolicyService extends AgentService {

    /**
     * Retrieves the type of service this policy service represents.
     *
     * @return the {@link PolicyType} representing the type of this policy service.
     */
    PolicyType getPolicyType();

    String getName();
}


