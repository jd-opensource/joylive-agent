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

import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;
import org.apache.rocketmq.client.consumer.store.OffsetStore;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.hook.ConsumeMessageHook;
import org.apache.rocketmq.client.impl.consumer.DefaultLitePullConsumerImpl;
import org.apache.rocketmq.client.impl.consumer.RebalanceImpl;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.client.trace.TraceDispatcher;
import org.apache.rocketmq.client.trace.hook.ConsumeMessageTraceHookImpl;
import org.apache.rocketmq.common.ServiceState;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.Collection;
import java.util.List;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;
import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.setValue;

/**
 * A lightweight RocketMQ pull consumer client implementing {@link DbConnection}.
 * Manages consumer lifecycle, reconnection, and message queue seeking.
 */
public class LitePullConsumerClient extends AbstractMQConsumerClient<DefaultLitePullConsumer> {

    private final DefaultLitePullConsumerImpl consumerImpl;

    public LitePullConsumerClient(DefaultLitePullConsumer consumer, ClusterRedirect address) {
        super(consumer, address);
        this.consumerImpl = getQuietly(consumer, "defaultLitePullConsumerImpl");
        this.rebalanceImpl = getQuietly(consumerImpl, "rebalanceImpl");
        consumerImpl.registerConsumeMessageHook(new TimestampHook(timestamps));
    }

    @Override
    protected void doClose() {
        target.shutdown();
    }

    @Override
    protected void doStart() throws MQClientException {
        // reset to restart
        reset();
        target.start();
        seek();
    }

    @Override
    protected Collection<MessageQueue> doFetchQueues(String topic) throws MQClientException {
        return target.fetchMessageQueues(topic);
    }

    @Override
    protected void doSeek(MessageQueue queue, long timestamp) throws MQClientException {
        Long offset = target.offsetForTimestamp(queue, timestamp);
        if (offset != null) {
            target.seek(queue, offset);
        } else {
            target.seekToBegin(queue);
        }
    }

    private void reset() {
        // reset state
        setValue(consumerImpl, "serviceState", ServiceState.CREATE_JUST);
        // reset trace
        resetTrace();
        // reset offset store
        resetOffsetStore();
    }

    private void resetTrace() {
        TraceDispatcher dispatcher = target.getTraceDispatcher();
        if (dispatcher != null) {
            List<ConsumeMessageHook> hooks = getQuietly(consumerImpl, "consumeMessageHookList");
            for (int i = hooks.size() - 1; i >= 0; i--) {
                ConsumeMessageHook hook = hooks.get(i);
                if (hook instanceof ConsumeMessageTraceHookImpl) {
                    TraceDispatcher traceDispatcher = getQuietly(hook, "traceDispatcher");
                    if (dispatcher == traceDispatcher) {
                        hooks.remove(i);
                        break;
                    }
                }
            }
        }
    }

    private void resetOffsetStore() {
        // reset offset store
        OffsetStore offsetStore = consumerImpl.getOffsetStore();
        if (offsetStore != null) {
            MQClientInstance mQClientFactory1 = getQuietly(offsetStore, "mQClientFactory");
            MQClientInstance mQClientFactory2 = getQuietly(consumerImpl, "mQClientFactory");
            if (mQClientFactory1 != null && mQClientFactory1 == mQClientFactory2) {
                // inner offset store
                setValue(target, "offsetStore", null);
            } else {
                // custom offset store
                RebalanceImpl rebalance = getQuietly(consumerImpl, "rebalanceImpl");
                rebalance.getProcessQueueTable().forEach((key, value) -> offsetStore.removeOffset(key));
            }
        }
    }
}
