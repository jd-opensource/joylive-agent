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

import com.jd.live.agent.bootstrap.util.Inclusion;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

public class RefreshConfig {

    @Getter
    @Setter
    private boolean environmentEnabled;

    @Getter
    @Setter
    private boolean beanEnabled = true;

    @Getter
    @Setter
    private Set<String> beanNames;

    @Getter
    @Setter
    private Set<String> beanClassPrefixes;

    @Getter
    @Setter
    private Set<String> ignoreKeys;

    @Getter
    @Setter
    private Set<String> ignoreKeyPrefixes;

    private transient Inclusion beanInclusion;

    private transient Inclusion ignoreInclusion;

    public boolean isEnabled(String beanName, Object bean) {
        if (beanInclusion == null) {
            beanInclusion = new Inclusion(beanNames, beanClassPrefixes, true);
        }
        return beanInclusion.test(beanName, n -> bean.getClass().getName());
    }

    public boolean isEnabled(String key) {
        if (ignoreInclusion == null) {
            ignoreInclusion = new Inclusion(ignoreKeys, ignoreKeyPrefixes);
        }
        return key != null && !ignoreInclusion.test(key);
    }

    public boolean isEmpty() {
        return (beanNames == null || beanNames.isEmpty()) && (beanClassPrefixes == null || beanClassPrefixes.isEmpty());
    }

}

