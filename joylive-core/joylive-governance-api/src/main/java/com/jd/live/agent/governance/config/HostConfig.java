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
package com.jd.live.agent.governance.config;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class HostConfig {

    private boolean enabled;

    private Map<String, String> services;

    public String getService(String host) {
        return host == null || services == null || services.isEmpty() ? null : services.get(host);
    }

    public String getService(String... names) {
        String result = null;
        if (names != null && services != null && !services.isEmpty()) {
            for (String name : names) {
                if (name != null) {
                    result = services.get(name);
                    if (result != null) {
                        break;
                    }
                }
            }
        }
        return result;
    }
}

