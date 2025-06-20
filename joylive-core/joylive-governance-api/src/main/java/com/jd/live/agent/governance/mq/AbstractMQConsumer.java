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
package com.jd.live.agent.governance.mq;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.option.Converts;
import com.jd.live.agent.governance.util.network.ClusterRedirect;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Abstract base class for MQ consumer clients.
 * Handles message queue seeking operations with timestamp-based offsets.
 */
public abstract class AbstractMQConsumer extends AbstractMQClient {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMQConsumer.class);

    /**
     * Default time offset (in milliseconds) for MQ message seeking.
     * Configurable via environment variable {@code MQ_SEEK_TIME_OFFSET}, defaults to 5 minutes.
     */
    protected static final long MQ_SEEK_TIME_OFFSET = Converts.getLong(System.getenv("MQ_SEEK_TIME_OFFSET"), 60 * 1000L);

    protected Map<String, AtomicLong> timestamps = new ConcurrentHashMap<>();

    public AbstractMQConsumer(Object target, ClusterRedirect address) {
        super(target, address);
    }

    @Override
    public MQClientRole getRole() {
        return MQClientRole.CONSUMER;
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
         * @throws Exception if queue discovery fails
         */
        Collection<MsgQueue> fetchQueues(String topic) throws Exception;

        /**
         * Adjusts consumption offset to specified timestamp.
         *
         * @param queue     target queue (non-null)
         * @param timestamp target position (milliseconds since epoch)
         * @throws Exception if offset adjustment fails
         */
        void seek(MsgQueue queue, long timestamp) throws Exception;

        /**
         * Batch adjusts offsets for all managed topics using timestamp function.
         *
         * @param timestampFunc function providing target timestamp per topic (non-null)
         */
        default void seek(Function<String, Long> timestampFunc) {
            getTopics().forEach(topic -> {
                long timestamp = timestampFunc.apply(topic);
                try {
                    Collection<MsgQueue> queues = fetchQueues(topic);
                    for (MsgQueue queue : queues) {
                        try {
                            seek(queue, timestamp);
                        } catch (Exception e) {
                            logger.error("Failed to seek queue {} offset at {} to timestamp {}", queue, getAddress(), timestamp, e);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to seek topic {} offset at {} to timestamp {}", topic, getAddress(), timestamp, e);
                }
            });
        }
    }
}
