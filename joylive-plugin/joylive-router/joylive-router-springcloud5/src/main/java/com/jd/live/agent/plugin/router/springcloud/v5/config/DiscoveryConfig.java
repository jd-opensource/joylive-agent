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
package com.jd.live.agent.plugin.router.springcloud.v5.config;

import com.jd.live.agent.governance.invoke.InvocationContext;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class DiscoveryConfig {

    private Set<String> disables = new HashSet<>();

    public boolean isDisabled(String type) {
        return type != null && disables.contains(type);
    }

    public void initialize(InvocationContext context) {
        if (context.isLiveEnabled()) {
            disables.add("org.springframework.cloud.loadbalancer.core.ZonePreferenceServiceInstanceListSupplier");
            disables.add("org.springframework.cloud.loadbalancer.core.SubsetServiceInstanceListSupplier");
            disables.add("org.springframework.cloud.loadbalancer.core.SameInstancePreferenceServiceInstanceListSupplier");
            disables.add("org.springframework.cloud.loadbalancer.core.RequestBasedStickySessionServiceInstanceListSupplier");
        }
        if (context.isFlowControlEnabled()) {
            disables.add("org.springframework.cloud.loadbalancer.core.SameInstancePreferenceServiceInstanceListSupplier");
            disables.add("org.springframework.cloud.loadbalancer.core.RequestBasedStickySessionServiceInstanceListSupplier");
            disables.add("org.springframework.cloud.loadbalancer.core.RetryAwareServiceInstanceListSupplier");
            disables.add("org.springframework.cloud.loadbalancer.core.WeightedServiceInstanceListSupplier");
        }
    }
}
