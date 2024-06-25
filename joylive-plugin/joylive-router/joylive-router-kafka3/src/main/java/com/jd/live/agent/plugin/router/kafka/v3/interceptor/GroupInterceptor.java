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
package com.jd.live.agent.plugin.router.kafka.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.interceptor.AbstractMessageInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;

import java.util.Map;
import java.util.Properties;

import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;

/**
 * ConsumerConfigInterceptor
 *
 * @since 1.0.0
 */
public class GroupInterceptor extends AbstractMessageInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(GroupInterceptor.class);

    public GroupInterceptor(InvocationContext context) {
        super(context);
    }

    /**
     * Enhanced logic before method execution. This method is called before the
     * target method is executed.
     *
     * @param ctx The execution context of the method being intercepted.
     * @see org.apache.kafka.clients.consumer.ConsumerConfig#ConsumerConfig(Properties)
     * @see org.apache.kafka.clients.consumer.ConsumerConfig#ConsumerConfig(Map)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        Object[] arguments = ctx.getArguments();
        if (arguments[0] instanceof Properties) {
            configure((Properties) arguments[0]);
        } else if (arguments[0] instanceof Map) {
            configure((Map) arguments[0]);
        }
    }

    private void configure(Properties properties) {
        String group = properties.getProperty(GROUP_ID_CONFIG);
        String newGroup = getGroup(group, null);
        properties.put(GROUP_ID_CONFIG, newGroup);
        if (!newGroup.equals(group)) {
            logger.info("Change consumer group of " + group + " to group " + newGroup);
        }
    }

    private void configure(Map map) {
        String group = (String) map.get(GROUP_ID_CONFIG);
        String newGroup = getGroup(group, null);
        map.put(GROUP_ID_CONFIG, newGroup);
        if (!newGroup.equals(group)) {
            logger.info("Change consumer group of " + group + " to group " + newGroup);
        }
    }

}
