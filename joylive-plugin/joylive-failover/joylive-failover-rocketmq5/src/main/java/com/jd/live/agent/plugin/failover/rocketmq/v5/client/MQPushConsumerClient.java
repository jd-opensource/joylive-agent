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
package com.jd.live.agent.plugin.failover.rocketmq.v5.client;

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
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.RPCHook;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;
import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.setValue;

public class MQPushConsumerClient extends AbstractMQConsumerClient<DefaultMQPushConsumer> {

    private static final String FIELD_CONSUMER_IMPL = "defaultMQPushConsumerImpl";

    public MQPushConsumerClient(DefaultMQPushConsumer consumer, ClusterRedirect address) {
        super(consumer, address);
        addMessageHook(getQuietly(consumer, FIELD_CONSUMER_IMPL));
    }

    @Override
    protected void doClose() {
        target.shutdown();
    }

    @Override
    protected void doStart() throws MQClientException {
        // reset to restart
        Seeker seeker = reset();
        boolean paused = seeker.isPaused();
        try {
            seeker.pause();
            target.start();
            seek(seeker);
        } finally {
            if (!paused) {
                seeker.resume();
            }
        }
    }

    /**
     * Resets consumer while maintaining subscriptions and tracing.
     *
     * @return seeker with original subscriptions and new consumer (never null)
     */
    private Seeker reset() {
        // capture topics
        DefaultMQPushConsumerImpl oldConsumerImpl = getQuietly(target, FIELD_CONSUMER_IMPL);
        // reset offset store
        resetOffsetStore(oldConsumerImpl);
        // recreate consumer impl
        RPCHook oldRpcHook = getQuietly(oldConsumerImpl, FIELD_RPC_HOOK);
        DefaultMQPushConsumerImpl newConsumerImpl = new DefaultMQPushConsumerImpl(target, oldRpcHook);
        newConsumerImpl.getSubscriptionInner().putAll(oldConsumerImpl.getSubscriptionInner());
        addMessageHook(newConsumerImpl);
        setValue(target, FIELD_CONSUMER_IMPL, newConsumerImpl);
        // reset trace
        resetTrace(oldRpcHook, newConsumerImpl);
        return new PushConsumerSeeker(newConsumerImpl);
    }

    /**
     * Registers timestamp tracking hook on pull consumer.
     */
    private void addMessageHook(DefaultMQPushConsumerImpl consumerImpl) {
        consumerImpl.registerConsumeMessageHook(new TimestampHook(timestamps));
    }

    /**
     * Resets the offset store for the specified consumer implementation.
     *
     * @param consumerImpl the push consumer implementation to reset (non-null)
     */
    private void resetOffsetStore(DefaultMQPushConsumerImpl consumerImpl) {
        // reset offset store
        OffsetStore offsetStore = consumerImpl.getOffsetStore();
        if (offsetStore != null) {
            MQClientInstance mQClientFactory1 = getQuietly(offsetStore, FIELD_CLIENT_FACTORY);
            MQClientInstance mQClientFactory2 = consumerImpl.getmQClientFactory();
            if (mQClientFactory1 != null && mQClientFactory1 == mQClientFactory2) {
                // inner offset store
                setValue(target, FIELD_OFFSET_STORE, null);
            } else {
                // custom offset store
                RebalanceImpl rebalance = consumerImpl.getRebalanceImpl();
                rebalance.getProcessQueueTable().forEach((key, value) -> offsetStore.removeOffset(key));
            }
        }
    }

    /**
     * Replaces the trace dispatcher if it exists and is of type AsyncTraceDispatcher.
     *
     * @param rpcHook      original hook to preserve
     * @param consumerImpl new consumer instance to associate
     */
    private void resetTrace(RPCHook rpcHook, DefaultMQPushConsumerImpl consumerImpl) {
        TraceDispatcher dispatcher = target.getTraceDispatcher();
        if (dispatcher instanceof AsyncTraceDispatcher) {
            // create new trace dispatcher
            setValue(target, FIELD_TRACE_DISPATCHER, createTraceDispatcher((AsyncTraceDispatcher) dispatcher, rpcHook, consumerImpl));
        }
    }

    /**
     * Creates a new trace dispatcher instance and migrates tracing hooks.
     *
     * @param dispatcher   old dispatcher instance (for configuration)
     * @param rpcHook      original hook to preserve
     * @param consumerImpl new consumer to associate
     * @return new AsyncTraceDispatcher instance
     */
    private TraceDispatcher createTraceDispatcher(AsyncTraceDispatcher dispatcher, RPCHook rpcHook, DefaultMQPushConsumerImpl consumerImpl) {
        AsyncTraceDispatcher result = new AsyncTraceDispatcher(target.getConsumerGroup(), TraceDispatcher.Type.CONSUME, dispatcher.getTraceTopicName(), rpcHook);
        result.setHostConsumer(consumerImpl);
        List<ConsumeMessageHook> hooks = getQuietly(consumerImpl, FIELD_HOOK_LIST);
        for (int i = hooks.size() - 1; i >= 0; i--) {
            ConsumeMessageHook hook = hooks.get(i);
            if (hook instanceof ConsumeMessageTraceHookImpl) {
                TraceDispatcher traceDispatcher = getQuietly(hook, FIELD_TRACE_DISPATCHER);
                if (dispatcher == traceDispatcher) {
                    hooks.remove(i);
                }
            }
        }
        consumerImpl.registerConsumeMessageHook(new ConsumeMessageTraceHookImpl(result));
        return result;
    }

    /**
     * Seeker implementation backed by a push consumer instance.
     * <p>
     * Maintains topic subscriptions and delegates queue operations to the underlying consumer.
     */
    private static class PushConsumerSeeker implements Seeker {

        private final DefaultMQPushConsumerImpl consumerImpl;

        private final RebalanceImpl rebalanceImpl;

        PushConsumerSeeker(DefaultMQPushConsumerImpl consumerImpl) {
            this.consumerImpl = consumerImpl;
            this.rebalanceImpl = consumerImpl.getRebalanceImpl();
        }

        @Override
        public boolean isPaused() {
            return consumerImpl.isPause();
        }

        @Override
        public void pause() {
            consumerImpl.suspend();
        }

        @Override
        public void resume() {
            consumerImpl.resume();
        }

        @Override
        public String getAddress() {
            return consumerImpl.getDefaultMQPushConsumer().getNamesrvAddr();
        }

        @Override
        public Set<String> getTopics() {
            return rebalanceImpl.getSubscriptionInner().keySet();
        }

        @Override
        public void rebalance() {
            rebalanceImpl.doRebalance(consumerImpl.isConsumeOrderly());
        }

        @Override
        public Collection<MessageQueue> fetchQueues(String topic) throws MQClientException {
            return consumerImpl.fetchSubscribeMessageQueues(topic);
        }

        @Override
        public void seek(MessageQueue queue, long timestamp) throws MQClientException {
            consumerImpl.updateConsumeOffset(queue, consumerImpl.searchOffset(queue, timestamp));
        }
    }

}
