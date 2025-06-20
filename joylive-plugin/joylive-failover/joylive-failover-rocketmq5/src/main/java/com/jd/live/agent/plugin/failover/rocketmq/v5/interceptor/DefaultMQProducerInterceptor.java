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
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.interceptor.AbstractMQFailoverInterceptor;
import com.jd.live.agent.governance.mq.MQClient;
import com.jd.live.agent.governance.mq.MQClientConfig;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import com.jd.live.agent.plugin.failover.rocketmq.v5.client.ProducerClient;
import com.jd.live.agent.plugin.failover.rocketmq.v5.client.RocketMQConfig;

/**
 * DefaultMQProducerInterceptor
 */
public class DefaultMQProducerInterceptor extends AbstractMQFailoverInterceptor<MQClient> {

    public DefaultMQProducerInterceptor(PolicySupplier policySupplier, Application application, GovernanceConfig governanceConfig, Publisher<DatabaseEvent> publisher, Timer timer) {
        super(policySupplier, application, governanceConfig, publisher, timer);
    }

    @Override
    protected MQClientConfig getClientConfig(ExecutableContext ctx) {
        return new RocketMQConfig(ctx.getTarget());
    }

    @Override
    protected MQClient createClient(ExecutableContext ctx, ClusterRedirect redirect) {
        return new ProducerClient(ctx.getTarget(), redirect);
    }
}
