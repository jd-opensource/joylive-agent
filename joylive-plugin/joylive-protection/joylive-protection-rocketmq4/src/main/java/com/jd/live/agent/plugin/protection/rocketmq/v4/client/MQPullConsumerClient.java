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
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.store.OffsetStore;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.consumer.DefaultMQPullConsumerImpl;
import org.apache.rocketmq.client.impl.consumer.RebalanceImpl;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.RPCHook;

import java.util.Collection;
import java.util.Set;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;
import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.setValue;

@Deprecated
public class MQPullConsumerClient extends AbstractMQConsumerClient<DefaultMQPullConsumer> {

    private static final String FIELD_CONSUMER_IMPL = "defaultMQPullConsumerImpl";

    public MQPullConsumerClient(DefaultMQPullConsumer consumer, ClusterRedirect address) {
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
        target.start();
        seek(seeker);

    }

    /**
     * Registers timestamp tracking hook on pull consumer.
     */
    private void addMessageHook(DefaultMQPullConsumerImpl consumerImpl) {
        consumerImpl.registerConsumeMessageHook(new TimestampHook(timestamps));
    }

    /**
     * Resets pull consumer while maintaining subscriptions.
     *
     * @return new seeker with original subscriptions
     */
    private Seeker reset() {
        // capture topics
        DefaultMQPullConsumerImpl oldConsumerImpl = getQuietly(target, FIELD_CONSUMER_IMPL);
        // reset offset store
        resetOffsetStore(oldConsumerImpl);
        // recreate consumer impl
        RPCHook oldRpcHook = getQuietly(oldConsumerImpl, FIELD_RPC_HOOK);
        DefaultMQPullConsumerImpl newConsumerImpl = new DefaultMQPullConsumerImpl(target, oldRpcHook);
        newConsumerImpl.getRebalanceImpl().getSubscriptionInner().putAll(oldConsumerImpl.getRebalanceImpl().getSubscriptionInner());
        addMessageHook(newConsumerImpl);
        setValue(target, FIELD_CONSUMER_IMPL, newConsumerImpl);

        return new PullConsumerSeeker(newConsumerImpl);
    }

    /**
     * Resets the offset store for the specified consumer implementation.
     *
     * @param consumerImpl the push consumer implementation to reset (non-null)
     */
    private void resetOffsetStore(DefaultMQPullConsumerImpl consumerImpl) {
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
                RebalanceImpl rebalance = consumerImpl.getRebalanceImpl();
                rebalance.getProcessQueueTable().forEach((key, value) -> offsetStore.removeOffset(key));
            }
        }
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
        public Collection<MessageQueue> fetchQueues(String topic) throws MQClientException {
            return consumerImpl.fetchSubscribeMessageQueues(topic);
        }

        @Override
        public void seek(MessageQueue queue, long timestamp) throws MQClientException {
            consumerImpl.updateConsumeOffset(queue, consumerImpl.searchOffset(queue, timestamp));
        }
    }
}
