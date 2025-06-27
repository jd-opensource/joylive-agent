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
package com.jd.live.agent.plugin.failover.rocketmq.v4.client;

import com.jd.live.agent.governance.db.mq.MsgQueue;
import com.jd.live.agent.governance.db.DbFailover;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.store.OffsetStore;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.hook.ConsumeMessageContext;
import org.apache.rocketmq.client.hook.ConsumeMessageHook;
import org.apache.rocketmq.client.impl.consumer.DefaultMQPullConsumerImpl;
import org.apache.rocketmq.client.impl.consumer.RebalanceImpl;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
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
import static com.jd.live.agent.plugin.failover.rocketmq.v4.client.RocketMQConfig.*;

@Deprecated
public class MQPullConsumerClient extends AbstractRocketMQConsumer {

    private static final String FIELD_CONSUMER_IMPL = "defaultMQPullConsumerImpl";

    public MQPullConsumerClient(Object consumer, DbFailover failover) {
        super(consumer, failover);
        addMessageHook(getQuietly(consumer, FIELD_CONSUMER_IMPL));
    }

    @Override
    protected void doClose() {
        ((DefaultMQPullConsumer) target).shutdown();
    }

    @Override
    protected void doStart() throws MQClientException {
        // reset to restart
        Seeker seeker = reset();
        ((DefaultMQPullConsumer) target).start();
        seek(seeker);
    }

    /**
     * Registers timestamp tracking hook on pull consumer.
     */
    private void addMessageHook(Object consumerImpl) {
        ((DefaultMQPullConsumerImpl) consumerImpl).registerConsumeMessageHook(new ConsumeMessageHook() {
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
     * Resets pull consumer while maintaining subscriptions.
     *
     * @return new seeker with original subscriptions
     */
    private Seeker reset() {
        // inline to fix classloader issue.
        // capture topics
        DefaultMQPullConsumerImpl oldConsumerImpl = getQuietly(target, FIELD_CONSUMER_IMPL);
        // reset offset store
        OffsetStore offsetStore = oldConsumerImpl.getOffsetStore();
        if (offsetStore != null) {
            MQClientInstance mQClientFactory1 = getQuietly(offsetStore, FIELD_CLIENT_FACTORY);
            MQClientInstance mQClientFactory2 = getQuietly(oldConsumerImpl, FIELD_CLIENT_FACTORY);
            if (mQClientFactory1 != null && mQClientFactory1 == mQClientFactory2) {
                // inner offset store
                setValue(target, FIELD_OFFSET_STORE, null);
            } else {
                // custom offset store
                RebalanceImpl oldRebalanceImpl = oldConsumerImpl.getRebalanceImpl();
                oldRebalanceImpl.getProcessQueueTable().forEach((key, value) -> offsetStore.removeOffset(key));
            }
        }
        // recreate consumer impl
        RPCHook oldRpcHook = getQuietly(oldConsumerImpl, FIELD_RPC_HOOK);
        DefaultMQPullConsumerImpl newConsumerImpl = new DefaultMQPullConsumerImpl((DefaultMQPullConsumer) target, oldRpcHook);
        newConsumerImpl.getRebalanceImpl().getSubscriptionInner().putAll(oldConsumerImpl.getRebalanceImpl().getSubscriptionInner());
        addMessageHook(newConsumerImpl);
        setValue(target, FIELD_CONSUMER_IMPL, newConsumerImpl);

        return new PullConsumerSeeker(newConsumerImpl);
    }

    /**
     * Pull consumer implementation of {@link Seeker}.
     */
    private static class PullConsumerSeeker implements Seeker {
        private final DefaultMQPullConsumerImpl consumerImpl;
        private final RebalanceImpl rebalanceImpl;

        PullConsumerSeeker(DefaultMQPullConsumerImpl consumerImpl) {
            this.consumerImpl = consumerImpl;
            this.rebalanceImpl = consumerImpl.getRebalanceImpl();
        }

        @Override
        public String getAddress() {
            return consumerImpl.getDefaultMQPullConsumer().getNamesrvAddr();
        }

        @Override
        public Set<String> getTopics() {
            return rebalanceImpl.getSubscriptionInner().keySet();
        }

        @Override
        public void rebalance() {
            rebalanceImpl.doRebalance(false);
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
