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
package com.jd.live.agent.governance.invoke.fault;

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.policy.service.fault.FaultInjectionPolicy;

/**
 * FaultInjection
 *
 * @since 1.4.0
 */
@Extensible
public interface FaultInjection {

    /**
     * Attempts to obtain a permit according to the specified fault injection policy.
     *
     * @param policy The fault injection policy to be used when attempting to acquire a permit.
     */
    void acquire(FaultInjectionPolicy policy);
}
