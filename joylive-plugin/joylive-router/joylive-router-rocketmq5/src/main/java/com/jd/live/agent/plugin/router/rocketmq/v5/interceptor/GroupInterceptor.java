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
package com.jd.live.agent.plugin.router.rocketmq.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.interceptor.AbstractMessageInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;

/**
 * GroupInterceptor
 *
 * @since 1.0.0
 */
public class GroupInterceptor extends AbstractMessageInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(GroupInterceptor.class);

    public GroupInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        String oldGroup = ctx.getArgument(0);
        String newGroup = getGroup(oldGroup, null);
        ctx.setArgument(0, newGroup);
        if (!newGroup.equals(oldGroup)) {
            logger.info("Change consumer group " + oldGroup + " to " + newGroup);
        }
    }

}
