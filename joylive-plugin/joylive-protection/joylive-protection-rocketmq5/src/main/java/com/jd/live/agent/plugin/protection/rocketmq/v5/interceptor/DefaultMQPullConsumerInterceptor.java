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
package com.jd.live.agent.plugin.protection.rocketmq.v5.interceptor;

import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import com.jd.live.agent.plugin.protection.rocketmq.v5.client.MQPullConsumerClient;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;

/**
 * DefaultMQPullConsumerInterceptor
 */
@Deprecated
public class DefaultMQPullConsumerInterceptor extends AbstractMQInterceptor<DefaultMQPullConsumer, MQPullConsumerClient> {

    public DefaultMQPullConsumerInterceptor(PolicySupplier policySupplier, Publisher<DatabaseEvent> publisher) {
        super(policySupplier, publisher);
    }

    @Override
    protected MQPullConsumerClient createClient(DefaultMQPullConsumer target, ClusterRedirect redirect) {
        return new MQPullConsumerClient(target, redirect);
    }

}
