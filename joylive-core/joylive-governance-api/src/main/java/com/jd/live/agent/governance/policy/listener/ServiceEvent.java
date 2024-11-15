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
package com.jd.live.agent.governance.policy.listener;

import com.jd.live.agent.core.config.ConfigEvent;
import com.jd.live.agent.governance.policy.service.MergePolicy;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
public class ServiceEvent extends ConfigEvent {

    @Setter
    private MergePolicy mergePolicy;

    private final Set<String> loadedServices = new HashSet<>();

    public ServiceEvent() {
    }

    @Builder(builderMethodName = "creator")
    public ServiceEvent(EventType type, String name, Object value, String description, String watcher, MergePolicy mergePolicy) {
        super(type, name, value, description, watcher);
        this.mergePolicy = mergePolicy;
    }

}
