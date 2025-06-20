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

import com.jd.live.agent.governance.mq.MsgQueue;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.store.OffsetStore;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.hook.ConsumeMessageContext;
import org.apache.rocketmq.client.hook.ConsumeMessageHook;
import org.apache.rocketmq.client.impl.consumer.DefaultMQPushConsumerImpl;
import org.apache.rocketmq.client.impl.consumer.RebalanceImpl;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.client.trace.AsyncTraceDispatcher;
import org.apache.rocketmq.client.trace.TraceDispatcher;
import org.apache.rocketmq.client.trace.hook.ConsumeMessageTraceHookImpl;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.RPCHook;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;
import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.setValue;
import static com.jd.live.agent.core.util.CollectionUtils.toSet;
import static com.jd.live.agent.plugin.failover.rocketmq.v5.client.RocketMQConfig.*;

public class MQPushConsumerClient extends AbstractRocketMQConsumer {

    private static final String FIELD_CONSUMER_IMPL = "defaultMQPushConsumerImpl";

    public MQPushConsumerClient(Object consumer, ClusterRedirect address) {
        super(consumer, address);
        addMessageHook(getQuietly(consumer, FIELD_CONSUMER_IMPL));
    }

    @Override
    protected void doClose() {
        ((DefaultMQPushConsumer) target).shutdown();
    }

    @Override
    protected void doStart() throws MQClientException {
        // reset to restart
        Seeker seeker = reset();
        boolean paused = seeker.isPaused();
        try {
            seeker.pause();
            ((DefaultMQPushConsumer) target).start();
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
        // inline to fix classloader issue.
        // capture topics
        DefaultMQPushConsumerImpl oldConsumerImpl = getQuietly(target, FIELD_CONSUMER_IMPL);
        // reset offset store
        OffsetStore offsetStore = oldConsumerImpl.getOffsetStore();
        if (offsetStore != null) {
            MQClientInstance mQClientFactory1 = getQuietly(offsetStore, FIELD_CLIENT_FACTORY);
            MQClientInstance mQClientFactory2 = oldConsumerImpl.getmQClientFactory();
            if (mQClientFactory1 != null && mQClientFactory1 == mQClientFactory2) {
                // inner offset store
                setValue(target, FIELD_OFFSET_STORE, null);
            } else {
                // custom offset store
                RebalanceImpl rebalance = oldConsumerImpl.getRebalanceImpl();
                rebalance.getProcessQueueTable().forEach((key, value) -> offsetStore.removeOffset(key));
            }
        }
        // recreate consumer impl
        DefaultMQPushConsumer consumer = (DefaultMQPushConsumer) target;
        RPCHook oldRpcHook = getQuietly(oldConsumerImpl, FIELD_RPC_HOOK);
        DefaultMQPushConsumerImpl newConsumerImpl = new DefaultMQPushConsumerImpl(consumer, oldRpcHook);
        newConsumerImpl.getSubscriptionInner().putAll(oldConsumerImpl.getSubscriptionInner());
        addMessageHook(newConsumerImpl);
        setValue(target, FIELD_CONSUMER_IMPL, newConsumerImpl);
        // reset trace
        TraceDispatcher oldDispatcher = consumer.getTraceDispatcher();
        if (oldDispatcher instanceof AsyncTraceDispatcher) {
            // create new trace dispatcher
            AsyncTraceDispatcher newDispatcher = new AsyncTraceDispatcher(consumer.getConsumerGroup(), TraceDispatcher.Type.CONSUME,
                    ((AsyncTraceDispatcher) oldDispatcher).getTraceTopicName(), oldRpcHook);
            newDispatcher.setHostConsumer(newConsumerImpl);
            List<ConsumeMessageHook> hooks = getQuietly(newConsumerImpl, FIELD_HOOK_LIST);
            for (int i = hooks.size() - 1; i >= 0; i--) {
                ConsumeMessageHook hook = hooks.get(i);
                if (hook instanceof ConsumeMessageTraceHookImpl) {
                    TraceDispatcher traceDispatcher = getQuietly(hook, FIELD_TRACE_DISPATCHER);
                    if (oldDispatcher == traceDispatcher) {
                        hooks.remove(i);
                    }
                }
            }
            newConsumerImpl.registerConsumeMessageHook(new ConsumeMessageTraceHookImpl(newDispatcher));
            setValue(target, FIELD_TRACE_DISPATCHER, newDispatcher);
        }
        return new PushConsumerSeeker(newConsumerImpl);
    }

    /**
     * Registers timestamp tracking hook on pull consumer.
     */
    private void addMessageHook(Object consumerImpl) {
        ((DefaultMQPushConsumerImpl) consumerImpl).registerConsumeMessageHook(new ConsumeMessageHook() {
            @Override
            public String hookName() {
                return "protection-hook";
            }

            @Override
            public void consumeMessageBefore(ConsumeMessageContext context) {

            }

            @Override
            public void consumeMessageAfter(ConsumeMessageContext context) {
                // Message consumption hook that tracks the latest message timestamp per topic.
                List<MessageExt> messages = context.getMsgList();
                if (messages != null && !messages.isEmpty()) {
                    MessageExt message = messages.get(messages.size() - 1);
                    long newTime = message.getStoreTimestamp();
                    AtomicLong last = timestamps.computeIfAbsent(message.getTopic(), k -> new AtomicLong(0));
                    long oldTime = last.get();
                    while (newTime > oldTime && !last.compareAndSet(oldTime, newTime)) {
                        oldTime = last.get();
                    }
                }
            }
        });
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
        public Collection<MsgQueue> fetchQueues(String topic) throws MQClientException {
            return toSet(consumerImpl.fetchSubscribeMessageQueues(topic), RocketMsgQueue::new);
        }

        @Override
        public void seek(MsgQueue queue, long timestamp) throws MQClientException {
            MessageQueue mq = ((RocketMsgQueue) queue).getQueue();
            long offset = consumerImpl.searchOffset(mq, timestamp);
            consumerImpl.updateConsumeOffset(mq, offset);
        }
    }

}
