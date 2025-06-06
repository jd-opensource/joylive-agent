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
package com.jd.live.agent.plugin.protection.rocketmq.v4.client;

import com.jd.live.agent.governance.util.network.ClusterRedirect;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.consumer.DefaultMQPushConsumerImpl;
import org.apache.rocketmq.common.ServiceState;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.Collection;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;
import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.setValue;

public class MQPushConsumerClient extends AbstractMQConsumerClient<DefaultMQPushConsumer> {

    private final DefaultMQPushConsumerImpl consumerImpl;

    public MQPushConsumerClient(DefaultMQPushConsumer consumer, ClusterRedirect address) {
        super(consumer, address);
        this.consumerImpl = getQuietly(consumer, "defaultMQPushConsumerImpl");
        this.rebalanceImpl = getQuietly(consumerImpl, "rebalanceImpl");
    }

    @Override
    protected void addMessageHook() {
        consumerImpl.registerConsumeMessageHook(new TimestampHook(timestamps));
    }

    @Override
    protected void doClose() {
        target.shutdown();
    }

    @Override
    protected void doStart() throws MQClientException {
        target.suspend();
        try {
            setValue(consumerImpl, "serviceState", ServiceState.CREATE_JUST);
            target.start();
            seek();
        } finally {
            target.resume();
        }
    }

    @Override
    protected Collection<MessageQueue> doFetchQueues(String topic) throws MQClientException {
        return target.fetchSubscribeMessageQueues(topic);
    }

    @Override
    protected void doSeek(MessageQueue queue, long timestamp) throws MQClientException {
        consumerImpl.updateConsumeOffset(queue, consumerImpl.searchOffset(queue, timestamp));
    }
}
