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
package com.jd.live.agent.plugin.protection.rocketmq.v5.client;

import com.google.common.base.Optional;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.client.impl.producer.TopicPublishInfo;
import org.apache.rocketmq.client.latency.MQFaultStrategy;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.ServiceState;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.concurrent.ConcurrentMap;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;
import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.setValue;

public class ProducerClient extends AbstractMQClient<DefaultMQProducer> {

    private final DefaultMQProducerImpl producerImpl;

    public ProducerClient(DefaultMQProducer producer, ClusterRedirect address) {
        super(producer, address);
        this.producerImpl = getQuietly(producer, "defaultMQProducerImpl");
    }

    @Override
    protected void doClose() {
        target.shutdown();
    }

    @Override
    protected void doStart() throws MQClientException {
        // reset to restart
        setValue(producerImpl, "serviceState", ServiceState.CREATE_JUST);
        setValue(producerImpl, "mqFaultStrategy", new MQFaultStrategy(target.cloneClientConfig(), this::resolve, this::detect));
        target.start();
    }

    private boolean detect(String endpoint, long timeoutMillis) {
        Optional<String> candidateTopic;
        ConcurrentMap<String, TopicPublishInfo> table = producerImpl.getTopicPublishInfoTable();
        if (table.isEmpty()) {
            candidateTopic = Optional.absent();
        } else {
            candidateTopic = Optional.of(table.keySet().iterator().next());
        }
        if (!candidateTopic.isPresent()) {
            return false;
        }
        try {
            MessageQueue mq = new MessageQueue(candidateTopic.get(), null, 0);
            producerImpl.getMqClientFactory().getMQClientAPIImpl().getMaxOffset(endpoint, mq, timeoutMillis);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String resolve(String name) {
        return producerImpl.getMqClientFactory().findBrokerAddressInPublish(name);
    }
}
