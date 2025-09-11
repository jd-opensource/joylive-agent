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
package com.jd.live.agent.core.event;

import com.jd.live.agent.core.extension.annotation.Extensible;

/**
 * Represents an extensible event bus interface, designed for managing and dispatching events
 * within a system. This interface defines core functionalities for creating message publishers
 * based on topics and stopping the event bus. It is marked as extensible, signifying that it
 * can be implemented in various ways to accommodate different event dispatching mechanisms.
 */
@Extensible("eventBus")
public interface EventBus {

    /**
     * Constant identifier for the event bus component.
     */
    String COMPONENT_EVENT_BUS = "eventBus";

    String CONFIG_PUBLISHER_PREFIX = "agent.publisher";

    String CONFIG_PUBLISHER_TYPE = CONFIG_PUBLISHER_PREFIX + ".type";

    String CONFIG_PUBLISHER_CONFIG = CONFIG_PUBLISHER_PREFIX + ".configs";

    /**
     * Priority order for the disruptor bus instance.
     */
    int ORDER_DISRUPTOR_BUS = 0;

    /**
     * Priority order for the JEventBus instance.
     */
    int ORDER_JEVENT_BUS = 100;

    /**
     * Creates and returns a publisher for a specific topic. This method allows for the
     * creation of message publishers that can publish events to listeners interested in
     * the specified topic. The generic type {@code <E>} represents the type of events
     * that the publisher will deal with.
     *
     * @param topic The name of the topic for which the publisher is created.
     * @param <E>   The type of events that the publisher will handle.
     * @return An instance of {@link Publisher} capable of publishing events to the specified topic.
     */
    <E> Publisher<E> getPublisher(String topic);

    /**
     * Subscribe to a topic with the given subscription.
     *
     * @param subscription the subscription to add, if null no operation performed
     * @param <E>          the type of subscription events
     */
    default <E> void subscribe(Subscription<E> subscription) {
        if (subscription != null) {
            Publisher<E> publisher = getPublisher(subscription.getTopic());
            publisher.addHandler(subscription);
        }
    }

    /**
     * Stops the event bus. This method is responsible for performing any necessary cleanup
     * or shutdown procedures to safely stop the event bus. Implementations may handle this
     * in different ways depending on the specific requirements of the event dispatching mechanism.
     */
    void stop();

}

