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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.hook.ConsumeMessageContext;
import org.apache.rocketmq.client.hook.ConsumeMessageHook;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Abstract base class for MQ consumer clients.
 * Handles message queue seeking operations with timestamp-based offsets.
 *
 * @param <T> Type of client configuration (must extend {@link ClientConfig})
 */
public abstract class AbstractMQConsumerClient<T extends ClientConfig> extends AbstractMQClient<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMQConsumerClient.class);

    protected static final String FIELD_REBALANCE_IMPL = "rebalanceImpl";
    protected static final String FIELD_HOOK_LIST = "consumeMessageHookList";
    protected static final String FIELD_CLIENT_FACTORY = "mQClientFactory";
    protected static final String FIELD_OFFSET_STORE = "offsetStore";

    protected Map<String, AtomicLong> timestamps = new ConcurrentHashMap<>();

    public AbstractMQConsumerClient(T target, ClusterRedirect address) {
        super(target, address);
    }

    @Override
    protected String getType() {
        return "consumer";
    }

    /**
     * Triggers rebalancing and seeks to adjusted timestamps for topics.
     * For each topic, uses either:
     * - Stored timestamp (if available) minus {@code MQ_SEEK_TIME_OFFSET}
     * - Current system time (for new/unseen topics)
     *
     * @param seeker the seeker instance handling the operations
     */
    protected void seek(Seeker seeker) {
        seeker.rebalance();
        seeker.seek(topic -> {
            AtomicLong value = timestamps.get(topic);
            return (value == null ? System.currentTimeMillis() : value.get()) - MQ_SEEK_TIME_OFFSET;
        });
    }

    /**
     * Message consumption hook that tracks the latest message timestamp per topic.
     */
    protected static class TimestampHook implements ConsumeMessageHook {

        private final Map<String, AtomicLong> timestamps;

        TimestampHook(Map<String, AtomicLong> timestamps) {
            this.timestamps = timestamps;
        }

        @Override
        public String hookName() {
            return "protection-hook";
        }

        @Override
        public void consumeMessageBefore(ConsumeMessageContext context) {

        }

        @Override
        public void consumeMessageAfter(ConsumeMessageContext context) {
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
    }

    /**
     * Message queue position tracker with topic-specific operations.
     *
     * <p>Implementations provide queue discovery and offset adjustment capabilities.
     */
    protected interface Seeker {

        /**
         * Checks if the current instance is paused.
         *
         * @return true if paused, false otherwise
         */
        default boolean isPaused() {
            return false;
        }

        /**
         * Pauses operations of this instance.
         */
        default void pause() {

        }

        /**
         * Resumes previously paused operations.
         */
        default void resume() {

        }

        /**
         * Gets the network address associated with this instance.
         *
         * @return address string (format depends on implementation)
         */
        String getAddress();

        /**
         * @return comma-separated topics this seeker manages
         */
        Set<String> getTopics();

        /**
         * Performs resource rebalancing according to current load or configuration.
         * Default implementation does nothing (no-op).
         */
        default void rebalance() {

        }

        /**
         * Retrieves all message queues for a topic.
         *
         * @param topic target topic (non-null)
         * @return active message queues (never null, may be empty)
         * @throws MQClientException if queue discovery fails
         */
        Collection<MessageQueue> fetchQueues(String topic) throws MQClientException;

        /**
         * Adjusts consumption offset to specified timestamp.
         *
         * @param queue     target queue (non-null)
         * @param timestamp target position (milliseconds since epoch)
         * @throws MQClientException if offset adjustment fails
         */
        void seek(MessageQueue queue, long timestamp) throws MQClientException;

        /**
         * Batch adjusts offsets for all managed topics using timestamp function.
         *
         * @param timestampFunc function providing target timestamp per topic (non-null)
         */
        default void seek(Function<String, Long> timestampFunc) {
            getTopics().forEach(topic -> {
                long timestamp = timestampFunc.apply(topic);
                try {
                    Collection<MessageQueue> queues = fetchQueues(topic);
                    for (MessageQueue queue : queues) {
                        try {
                            seek(queue, timestamp);
                        } catch (MQClientException e) {
                            logger.error("Failed to seek queue {}@{}@{} offset at {} to timestamp {}",
                                    queue.getQueueId(), queue.getTopic(), queue.getBrokerName(), getAddress(), timestamp, e);
                        }
                    }
                } catch (MQClientException e) {
                    logger.error("Failed to seek topic {} offset at {} to timestamp {}", topic, getAddress(), timestamp, e);
                }
            });
        }
    }
}
