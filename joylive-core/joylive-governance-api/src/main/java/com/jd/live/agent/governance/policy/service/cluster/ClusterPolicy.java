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
package com.jd.live.agent.governance.policy.service.cluster;

import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.PolicyInherit.PolicyInheritWithId;
import com.jd.live.agent.governance.policy.service.annotation.Consumer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Consumer
public class ClusterPolicy extends PolicyId implements PolicyInheritWithId<ClusterPolicy> {

    private String type;

    private RetryPolicy retryPolicy;

    public ClusterPolicy() {
    }

    public ClusterPolicy(String type) {
        this.type = type;
    }

    public ClusterPolicy(String type, RetryPolicy retryPolicy) {
        this.type = type;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public void supplement(ClusterPolicy source) {
        if (retryPolicy != null) {
            retryPolicy.setId(id);
        }
        if (source != null) {
            if (retryPolicy == null) {
                retryPolicy = new RetryPolicy();
                retryPolicy.setId(id);
            }
            retryPolicy.supplement(source.retryPolicy);
        }
    }

    public void cache() {
        if (retryPolicy != null) {
            retryPolicy.cache();
        }
    }
}
