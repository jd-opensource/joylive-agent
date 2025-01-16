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

import java.util.Set;

@Getter
@Setter
public class RefreshConfig {

    private boolean environmentEnabled;

    private boolean beanEnabled = true;

    private Set<String> beanNames;

    private Set<String> beanClassPrefixes;

    public boolean isEnabled(String beanName, Object bean) {
        if (beanName != null && beanNames != null && !beanName.isEmpty() && !beanNames.contains(beanName)) {
            return false;
        } else if (bean != null && beanClassPrefixes != null && !beanClassPrefixes.isEmpty()) {
            String className = bean.getClass().getName();
            for (String prefix : beanClassPrefixes) {
                if (className.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    public boolean isEmpty() {
        return (beanNames == null || beanNames.isEmpty()) && (beanClassPrefixes == null || beanClassPrefixes.isEmpty());
    }

}

