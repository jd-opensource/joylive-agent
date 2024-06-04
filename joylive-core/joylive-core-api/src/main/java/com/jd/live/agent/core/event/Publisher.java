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

import java.util.concurrent.TimeUnit;

/**
 * Represents a publisher in a message publishing system. This interface defines the operations
 * for managing event handlers and publishing events to those handlers. It is designed to work
 * with a specific topic and allows for the addition, removal, and offering of events to handlers
 * that are interested in the publisher's topic.
 *
 * @param <E> The type of events this publisher deals with.
 */
public interface Publisher<E> {

    /**
     * Topic identifier for system-related events.
     */
    String SYSTEM = "system";

    /**
     * Topic identifier for configuration-related events.
     */
    String CONFIG = "config";

    /**
     * Topic identifier for enhancement-related events.
     */
    String ENHANCE = "enhance";

    /**
     * Topic identifier for traffic-related events.
     */
    String TRAFFIC = "traffic";

    /**
     * Policy identifier for subscribers.
     */
    String POLICY_SUBSCRIBER = "policySubscriber";

    /**
     * Retrieves the topic associated with this publisher.
     *
     * @return The topic of this publisher.
     */
    String getTopic();

    /**
     * Adds an event handler to this publisher. The handler will be notified of events
     * published to the topic this publisher manages.
     *
     * @param handler The event handler to be added.
     * @return {@code true} if the handler was successfully added, {@code false} otherwise.
     */
    boolean addHandler(EventHandler<E> handler);

    /**
     * Removes an event handler from this publisher. The handler will no longer receive
     * events from this publisher.
     *
     * @param handler The event handler to be removed.
     * @return {@code true} if the handler was successfully removed, {@code false} otherwise.
     */
    boolean removeHandler(EventHandler<E> handler);

    /**
     * Offers an event to this publisher. The event will be dispatched to all handlers
     * registered with this publisher.
     *
     * @param event The event to be offered.
     * @return {@code true} if the event was successfully offered, {@code false} otherwise.
     */
    boolean offer(Event<E> event);

    /**
     * Offers an event to the event queue.
     *
     * @param event the event to be offered
     * @return {@code true} if the event was successfully added to the queue, {@code false} otherwise
     */
    default boolean offer(E event) {
        return offer(new Event<>(event));
    }

    /**
     * Offers an event to this publisher with a timeout. The event will be dispatched
     * to all handlers registered with this publisher if it is accepted within the specified
     * timeout period.
     *
     * @param event    The event to be offered.
     * @param timeout  The maximum time to wait for the event to be accepted.
     * @param timeUnit The time unit of the timeout argument.
     * @return {@code true} if the event was successfully offered, {@code false} otherwise.
     */
    boolean offer(Event<E> event, long timeout, TimeUnit timeUnit);

    /**
     * Offers an event to the event queue, waiting up to the specified timeout if necessary for space to become available.
     *
     * @param event    the event to be offered
     * @param timeout  how long to wait before giving up, in units of {@code timeUnit}
     * @param timeUnit the time unit of the {@code timeout} argument
     * @return {@code true} if the event was successfully added to the queue, {@code false} if the specified waiting time elapses before space is available
     * @throws InterruptedException if interrupted while waiting
     */
    default boolean offer(E event, long timeout, TimeUnit timeUnit) {
        return offer(new Event<>(event), timeout, timeUnit);
    }
}

