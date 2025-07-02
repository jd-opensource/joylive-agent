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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Load limit policy
 */
public class LoadLimitPolicy extends AbstractLimitPolicy implements LimitPolicy, PolicyInheritWithIdGen<LoadLimitPolicy> {

    @Getter
    @Setter
    @Deprecated
    private Integer cpuUsage;

    @Getter
    @Setter
    @Deprecated
    private Integer loadUsage;

    @Getter
    @Setter
    private List<LoadLimitThrottle> throttles;

    private transient List<LoadLimitThrottle> cache;

    public LoadLimitPolicy() {
    }

    public LoadLimitPolicy(String name) {
        super(name);
    }

    public boolean isEmpty() {
        return cache == null || cache.isEmpty();
    }

    public int getRatio(LoadMetric metric) {
        return LoadLimitThrottle.getRatio(metric, cache);
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
        if (throttles == null && source.throttles != null) {
            throttles = new ArrayList<>(source.throttles);
        }
    }

    public void cache() {
        cache = throttles == null ? new ArrayList<>() : new ArrayList<>(throttles);
        if (cpuUsage != null || loadUsage != null) {
            cache.add(new LoadLimitThrottle(cpuUsage, loadUsage, 100));
        }
        Collections.sort(cache);
    }
}

