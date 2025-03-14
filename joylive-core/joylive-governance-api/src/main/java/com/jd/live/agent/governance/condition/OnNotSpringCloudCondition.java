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
package com.jd.live.agent.governance.condition;

import com.jd.live.agent.core.extension.condition.ConditionContext;
import com.jd.live.agent.core.extension.condition.OnCondition;

import java.net.URL;

/**
 * on not spring cloud condition
 */
public class OnNotSpringCloudCondition extends OnCondition {

    String TYPE_LOAD_BALANCED = "org.springframework.cloud.client.loadbalancer.LoadBalanced";

    String CONFIG_SPRING_CLOUD_ENABLED = "agent.governance.springCloud";

    @Override
    public boolean match(final ConditionContext context) {
        String name = TYPE_LOAD_BALANCED;
        name = name.replace(".", "/") + ".class";
        URL url = context.getClassLoader().getResource(name);
        if (url == null) {
            return true;
        }
        String value = context.geConfig(CONFIG_SPRING_CLOUD_ENABLED);
        return !isEmpty(value) && !Boolean.parseBoolean(value);
    }
}
