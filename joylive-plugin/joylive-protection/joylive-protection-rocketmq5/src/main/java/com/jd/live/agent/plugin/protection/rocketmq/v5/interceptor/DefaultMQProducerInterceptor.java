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

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.interceptor.AbstractDbConnectionInterceptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import com.jd.live.agent.plugin.protection.rocketmq.v5.client.ProducerClient;
import org.apache.rocketmq.client.producer.DefaultMQProducer;

/**
 * DefaultMQProducerInterceptor
 */
public class DefaultMQProducerInterceptor extends AbstractDbConnectionInterceptor<DefaultMQProducer, ProducerClient> {

    public DefaultMQProducerInterceptor(PolicySupplier policySupplier, Publisher<DatabaseEvent> publisher) {
        super(policySupplier, publisher);
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        DefaultMQProducer producer = mc.getResult();
        String address = producer.getNamesrvAddr();
        if (address != null && !address.isEmpty()) {
            addConnection(new ProducerClient(producer, new ClusterRedirect(address)));
        }
    }

    @Override
    protected void redirectTo(ProducerClient client, ClusterAddress address) {
        client.reconnect(address);
        ClusterRedirect.redirect(client.getAddress().newAddress(address), consumer);
    }

}
