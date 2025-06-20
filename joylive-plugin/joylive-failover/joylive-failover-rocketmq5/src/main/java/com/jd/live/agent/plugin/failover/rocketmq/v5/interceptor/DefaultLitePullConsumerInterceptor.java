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
package com.jd.live.agent.plugin.failover.rocketmq.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.governance.interceptor.AbstractMQFailoverInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.mq.MQClient;
import com.jd.live.agent.governance.mq.MQClientConfig;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import com.jd.live.agent.plugin.failover.rocketmq.v5.client.LitePullConsumerClient;
import com.jd.live.agent.plugin.failover.rocketmq.v5.client.RocketMQConfig;

/**
 * DefaultLitePullConsumerInterceptor
 */
public class DefaultLitePullConsumerInterceptor extends AbstractMQFailoverInterceptor<MQClient> {

    public DefaultLitePullConsumerInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    protected MQClientConfig getClientConfig(ExecutableContext ctx) {
        return new RocketMQConfig(ctx.getTarget());
    }

    @Override
    protected MQClient createClient(ExecutableContext ctx, ClusterRedirect redirect) {
        return new LitePullConsumerClient(ctx.getTarget(), redirect);
    }
}
