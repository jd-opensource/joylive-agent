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

import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;
import org.apache.rocketmq.client.consumer.store.OffsetStore;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.consumer.DefaultLitePullConsumerImpl;
import org.apache.rocketmq.client.impl.consumer.RebalanceImpl;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.RPCHook;

import java.util.Collection;
import java.util.Set;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;
import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.setValue;

/**
 * A lightweight RocketMQ pull consumer client implementing {@link DbConnection}.
 * Manages consumer lifecycle, reconnection, and message queue seeking.
 */
public class LitePullConsumerClient extends AbstractMQConsumerClient<DefaultLitePullConsumer> {

    private static final String FIELD_CONSUMER_IMPL = "defaultLitePullConsumerImpl";

    public LitePullConsumerClient(DefaultLitePullConsumer consumer, ClusterRedirect address) {
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
        // reset to restart
        Seeker seeker = reset();
        target.start();
        seek(seeker);
    }

    /**
     * Adds timestamp tracking hook to consumer.
     */
    private void addMessageHook(DefaultLitePullConsumerImpl consumerImpl) {
        consumerImpl.registerConsumeMessageHook(new TimestampHook(timestamps));
    }

    /**
     * Recreates consumer while preserving subscriptions.
     *
     * @return new seeker with preserved state
     */
    private Seeker reset() {
        // capture topics
        DefaultLitePullConsumerImpl oldConsumerImpl = getQuietly(target, FIELD_CONSUMER_IMPL);
        RebalanceImpl oldRebalanceImpl = getQuietly(oldConsumerImpl, FIELD_REBALANCE_IMPL);
        // reset offset store
        resetOffsetStore(oldConsumerImpl, oldRebalanceImpl);
        // recreate consumer impl
        RPCHook oldRpcHook = getQuietly(oldConsumerImpl, FIELD_RPC_HOOK);
        DefaultLitePullConsumerImpl newConsumerImpl = new DefaultLitePullConsumerImpl(target, oldRpcHook);
        RebalanceImpl newRebalanceImpl = getQuietly(newConsumerImpl, FIELD_REBALANCE_IMPL);
        newRebalanceImpl.getSubscriptionInner().putAll(oldRebalanceImpl.getSubscriptionInner());
        addMessageHook(newConsumerImpl);
        setValue(target, FIELD_CONSUMER_IMPL, newConsumerImpl);
        // trace dispatcher will be recreated in start method.
        return new LitePullConsumerSeeker(newConsumerImpl, newRebalanceImpl);
    }

    /**
     * Resets the offset store for the specified consumer implementation.
     *
     * @param consumerImpl the push consumer implementation to reset (non-null)
     */
    private void resetOffsetStore(DefaultLitePullConsumerImpl consumerImpl, RebalanceImpl rebalanceImpl) {
        // reset offset store
        OffsetStore offsetStore = consumerImpl.getOffsetStore();
        if (offsetStore != null) {
            MQClientInstance mQClientFactory1 = getQuietly(offsetStore, FIELD_CLIENT_FACTORY);
            MQClientInstance mQClientFactory2 = getQuietly(consumerImpl, FIELD_CLIENT_FACTORY);
            if (mQClientFactory1 != null && mQClientFactory1 == mQClientFactory2) {
                // inner offset store
                setValue(target, FIELD_OFFSET_STORE, null);
            } else {
                // custom offset store
                rebalanceImpl.getProcessQueueTable().forEach((key, value) -> offsetStore.removeOffset(key));
            }
        }
    }

    /**
     * Seeker implementation for lite pull consumer.
     */
    private static class LitePullConsumerSeeker implements Seeker {

        private final DefaultLitePullConsumerImpl consumerImpl;
        private final RebalanceImpl rebalanceImpl;

        LitePullConsumerSeeker(DefaultLitePullConsumerImpl consumerImpl, RebalanceImpl rebalanceImpl) {
            this.consumerImpl = consumerImpl;
            this.rebalanceImpl = rebalanceImpl;
        }

        @Override
        public String getAddress() {
            return consumerImpl.getDefaultLitePullConsumer().getNamesrvAddr();
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
        public Collection<MessageQueue> fetchQueues(String topic) throws MQClientException {
            return consumerImpl.fetchMessageQueues(topic);
        }

        @Override
        public void seek(MessageQueue queue, long timestamp) throws MQClientException {
            consumerImpl.updateConsumeOffset(queue, consumerImpl.searchOffset(queue, timestamp));
        }
    }
}
