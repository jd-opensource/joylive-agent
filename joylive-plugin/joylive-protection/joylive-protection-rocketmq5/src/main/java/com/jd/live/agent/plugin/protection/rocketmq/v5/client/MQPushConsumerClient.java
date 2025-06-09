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

import com.jd.live.agent.governance.util.network.ClusterRedirect;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.store.OffsetStore;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.hook.ConsumeMessageHook;
import org.apache.rocketmq.client.impl.consumer.DefaultMQPushConsumerImpl;
import org.apache.rocketmq.client.impl.consumer.RebalanceImpl;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.client.trace.AsyncTraceDispatcher;
import org.apache.rocketmq.client.trace.TraceDispatcher;
import org.apache.rocketmq.client.trace.hook.ConsumeMessageTraceHookImpl;
import org.apache.rocketmq.common.ServiceState;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.RPCHook;

import java.util.Collection;
import java.util.List;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;
import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.setValue;

public class MQPushConsumerClient extends AbstractMQConsumerClient<DefaultMQPushConsumer> {

    private final DefaultMQPushConsumerImpl consumerImpl;

    public MQPushConsumerClient(DefaultMQPushConsumer consumer, ClusterRedirect address) {
        super(consumer, address);
        this.consumerImpl = getQuietly(consumer, "defaultMQPushConsumerImpl");
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
        target.suspend();
        try {
            reset();
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

    private void reset() {
        // reset state
        setValue(consumerImpl, "serviceState", ServiceState.CREATE_JUST);
        setValue(consumerImpl, "pullAPIWrapper", null);
        // reset trace
        resetTrace();
        // reset offset store
        resetOffsetStore();
    }

    private void resetTrace() {
        TraceDispatcher dispatcher = target.getTraceDispatcher();
        if (dispatcher instanceof AsyncTraceDispatcher) {
            // create new trace dispatcher
            setValue(target, "traceDispatcher", createTraceDispatcher((AsyncTraceDispatcher) dispatcher));
        }
    }

    private void resetOffsetStore() {
        // reset offset store
        OffsetStore offsetStore = consumerImpl.getOffsetStore();
        if (offsetStore != null) {
            MQClientInstance mQClientFactory1 = getQuietly(offsetStore, "mQClientFactory");
            MQClientInstance mQClientFactory2 = consumerImpl.getmQClientFactory();
            if (mQClientFactory1 != null && mQClientFactory1 == mQClientFactory2) {
                // inner offset store
                setValue(target, "offsetStore", null);
            } else {
                // custom offset store
                RebalanceImpl rebalance = consumerImpl.getRebalanceImpl();
                rebalance.getProcessQueueTable().forEach((key, value) -> offsetStore.removeOffset(key));
            }
        }
    }

    private TraceDispatcher createTraceDispatcher(AsyncTraceDispatcher dispatcher) {
        RPCHook rpcHook = getQuietly(consumerImpl, "rpcHook");
        AsyncTraceDispatcher result = new AsyncTraceDispatcher(target.getConsumerGroup(), TraceDispatcher.Type.CONSUME, dispatcher.getTraceTopicName(), rpcHook);
        result.setHostConsumer(consumerImpl);
        List<ConsumeMessageHook> hooks = getQuietly(consumerImpl, "consumeMessageHookList");
        for (int i = hooks.size() - 1; i >= 0; i--) {
            ConsumeMessageHook hook = hooks.get(i);
            if (hook instanceof ConsumeMessageTraceHookImpl) {
                TraceDispatcher traceDispatcher = getQuietly(hook, "traceDispatcher");
                if (dispatcher == traceDispatcher) {
                    hooks.remove(i);
                }
            }
        }
        consumerImpl.registerConsumeMessageHook(new ConsumeMessageTraceHookImpl(result));
        return result;
    }

}
