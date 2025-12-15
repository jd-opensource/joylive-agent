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
package com.jd.live.agent.governance.subscription.config.gray;

import com.jd.live.agent.core.instance.Application;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

/**
 * Represents a configuration gray policy with IP-based and label-based matching rules.
 */
@Getter
@Setter
public class ConfigPolicy {

    private String name;

    private Set<String> applications;

    private Set<String> ips;

    private Map<String, String> labels;

    public boolean match(Application application) {
        if (application == null) {
            return false;
        }
        String localIp = application.getLocation().getIp();
        if (applications != null && !applications.isEmpty() && !applications.contains(application.getName())) {
            return false;
        } else if (ips != null && !ips.isEmpty() && !ips.contains(localIp)) {
            return false;
        }
        return true;
    }
}
