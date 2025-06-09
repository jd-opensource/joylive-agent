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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.hook.ConsumeMessageContext;
import org.apache.rocketmq.client.hook.ConsumeMessageHook;
import org.apache.rocketmq.client.impl.consumer.RebalanceImpl;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract base class for MQ consumer clients.
 * Handles message queue seeking operations with timestamp-based offsets.
 *
 * @param <T> Type of client configuration (must extend {@link ClientConfig})
 */
public abstract class AbstractMQConsumerClient<T extends ClientConfig> extends AbstractMQClient<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMQConsumerClient.class);

    protected Map<String, AtomicLong> timestamps = new ConcurrentHashMap<>();

    protected RebalanceImpl rebalanceImpl;

    public AbstractMQConsumerClient(T target, ClusterRedirect address) {
        super(target, address);
    }

    /**
     * Seeks all subscribed topics to current time minus {@code MQ_SEEK_TIME_OFFSET}.
     */
    protected void seek() {
        rebalanceImpl.getSubscriptionInner().forEach((k, v) -> seek(k, getOrDefault(k, MQ_SEEK_TIME_OFFSET)));
    }

    /**
     * Retrieves the stored timestamp for the given topic.
     * Returns current system time minus offset if topic not found.
     *
     * @param topic  target topic to query (case-sensitive)
     * @param offset value to subtract from the result (typically 0)
     * @return stored timestamp minus offset, or current time minus offset if topic absent
     */
    protected long getOrDefault(String topic, long offset) {
        AtomicLong value = timestamps.get(topic);
        return (value == null ? System.currentTimeMillis() : value.get()) - offset;
    }

    /**
     * Seeks to the specified timestamp for all queues of a topic.
     * Falls back to beginning if timestamp offset is unavailable.
     *
     * @param topic     the topic to seek
     * @param timestamp the target timestamp in milliseconds
     */
    protected void seek(String topic, long timestamp) {
        try {
            Collection<MessageQueue> queues = doFetchQueues(topic);
            for (MessageQueue queue : queues) {
                seek(queue, timestamp);
            }
        } catch (MQClientException e) {
            logger.error("Failed to seek {} offset to timestamp {}", topic, timestamp, e);
        }
    }

    /**
     * Implementation-specific queue retrieval.
     *
     * @param topic target topic
     * @return collection of message queues
     * @throws MQClientException if queue fetch fails
     */
    protected abstract Collection<MessageQueue> doFetchQueues(String topic) throws MQClientException;

    /**
     * Seeks the specified message queue to the given timestamp.
     * If no offset is found for the timestamp, falls back to the beginning of the queue.
     *
     * @param queue     the message queue to seek
     * @param timestamp the target timestamp in milliseconds
     */
    protected void seek(MessageQueue queue, long timestamp) {
        try {
            doSeek(queue, timestamp);
        } catch (MQClientException e) {
            logger.error("Failed to seek queue {}@{}@{} offset to timestamp {}",
                    queue.getQueueId(), queue.getTopic(), queue.getBrokerName(), timestamp, e);
        }
    }

    /**
     * Implementation-specific seek operation.
     *
     * @param queue     target message queue
     * @param timestamp target time in milliseconds
     * @throws MQClientException if seek operation fails
     */
    protected abstract void doSeek(MessageQueue queue, long timestamp) throws MQClientException;

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
}
