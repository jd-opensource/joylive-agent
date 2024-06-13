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

import java.io.Closeable;
import java.io.IOException;

/**
 * Represents an extensible interface for a subscription in an event-driven system.
 * This interface extends {@link EventHandler}, meaning it is capable of handling events
 * of a specific type. As a subscription, it is associated with a specific topic, allowing
 * it to receive and process events published to that topic. It is marked as extensible
 * with the "subscription" designation, indicating that it can be implemented in various
 * ways to accommodate different event handling and subscription mechanisms.
 *
 * @param <E> The type of events that this subscription will handle.
 */
@Extensible("subscription")
public interface Subscription<E> extends EventHandler<E>, Closeable {

    /**
     * Retrieves the topic associated with this subscription. The topic indicates which
     * events this subscription is interested in and will handle.
     *
     * @return The topic of this subscription.
     */
    String getTopic();

    @Override
    default void close() throws IOException {

    }
}

