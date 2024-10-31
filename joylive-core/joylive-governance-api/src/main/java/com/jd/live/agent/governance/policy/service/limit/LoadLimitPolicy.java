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
package com.jd.live.agent.governance.policy.service.limit;

import com.jd.live.agent.governance.policy.PolicyInherit.PolicyInheritWithIdGen;
import lombok.Getter;
import lombok.Setter;

/**
 * Load limit policy
 */
@Getter
@Setter
public class LoadLimitPolicy extends AbstractLimitPolicy implements LimitPolicy, PolicyInheritWithIdGen<LoadLimitPolicy> {

    private Integer cpuUsage;

    private Integer loadUsage;

    public LoadLimitPolicy() {
    }

    @Override
    public void supplement(LoadLimitPolicy source) {
        if (source == null) {
            return;
        }
        super.supplement(source);
        if (cpuUsage == null) {
            cpuUsage = source.cpuUsage;
        }
        if (loadUsage == null) {
            loadUsage = source.loadUsage;
        }
    }
}

