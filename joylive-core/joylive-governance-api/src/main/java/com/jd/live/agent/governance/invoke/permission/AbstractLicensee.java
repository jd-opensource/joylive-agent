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
package com.jd.live.agent.governance.invoke.permission;

import com.jd.live.agent.governance.policy.PolicyVersion;
import lombok.Getter;

/**
 * An abstract class representing a licensee with a policy version.
 *
 * @param <P> the type of policy version
 */
@Getter
public abstract class AbstractLicensee<P extends PolicyVersion> implements Licensee<P> {

    /**
     * The policy associated with the licensee.
     */
    protected P policy;

    @Override
    public void exchange(P policy) {
        P old = this.policy;
        if (policy != null && policy != old && policy.getVersion() == old.getVersion()) {
            doExchange(old, policy);
            this.policy = policy;
        }
    }

    /**
     * Performs the exchange of the policy from the older version to the newer version.
     * This method can be overridden by subclasses to add additional logic during the policy exchange.
     *
     * @param older the current policy version to be replaced
     * @param newer the new policy version to be set
     */
    protected void doExchange(P older, P newer) {

    }
}


