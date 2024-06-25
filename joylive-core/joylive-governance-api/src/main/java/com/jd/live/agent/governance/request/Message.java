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
package com.jd.live.agent.governance.request;

/**
 * Represents a message with various attributes such as topic, headers, and space IDs.
 */
public interface Message {

    String TOPIC_REPLY = "x-topic-reply";

    /**
     * Retrieves the topic of the message.
     *
     * @return the topic of the message.
     */
    String getTopic();

    /**
     * Retrieves the value of a specific header from the message.
     *
     * @param key the key of the header to retrieve.
     * @return the value of the specified header, or {@code null} if the header is not present.
     */
    String getHeader(String key);

    /**
     * Retrieves the live space ID associated with the message.
     *
     * @return the live space ID of the message.
     */
    String getLiveSpaceId();

    /**
     * Retrieves the rule ID associated with the message.
     *
     * @return the rule ID of the message.
     */
    String getRuleId();

    /**
     * Retrieves the variable associated with the message.
     *
     * @return the variable of the message.
     */
    String getVariable();

    /**
     * Retrieves the lane space ID associated with the message.
     *
     * @return the lane space ID of the message.
     */
    String getLaneSpaceId();

    /**
     * Retrieves the lane associated with the message.
     *
     * @return the lane of the message.
     */
    String getLane();

    /**
     * ProducerMessage is an interface that extends the Message interface.
     * It provides additional functionality for setting the topic name for a message.
     */
    interface ProducerMessage extends Message {
        /**
         * Sets the topic name.
         *
         * @param topic the new topic name to set.
         */
        void setTopic(String topic);

        /**
         * Sets a header for the message.
         *
         * @param key   the header key.
         * @param value the header value.
         */
        void setHeader(String key, String value);
    }
}


