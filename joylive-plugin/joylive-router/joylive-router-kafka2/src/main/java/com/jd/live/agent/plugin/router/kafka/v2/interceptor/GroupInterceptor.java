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
package com.jd.live.agent.plugin.router.kafka.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.interceptor.AbstractMessageInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;

import java.util.Map;

import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;

/**
 * GroupInterceptor
 *
 * @since 1.8.0
 */
public class GroupInterceptor extends AbstractMessageInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(GroupInterceptor.class);

    public GroupInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onEnter(ExecutableContext ctx) {
        Object arg = ctx.getArgument(0);
        // ConsumerConfig(Map<String,Object>)
        // ConsumerConfig(Properties)
        // ConsumerConfig(Map<?, ?> props, boolean doLog)
        if (arg instanceof Map) {
            Map map = (Map) arg;
            String oldGroup = (String) map.get(GROUP_ID_CONFIG);
            String newGroup = getGroup(oldGroup, null);
            if (!newGroup.equals(oldGroup)) {
                map.put(GROUP_ID_CONFIG, newGroup);
                logger.info("Change consumer group " + oldGroup + " to " + newGroup);
            }
        }
    }
}
