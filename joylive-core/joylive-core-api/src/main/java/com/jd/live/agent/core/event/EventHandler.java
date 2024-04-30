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

import java.util.List;

/**
 * Represents a functional interface for handling events. This interface is designed
 * to process a list of events of a specific type. Being a functional interface, it
 * facilitates the use of lambda expressions or method references to implement event
 * handling logic in a concise manner.
 *
 * @param <E> The type of the event data that this handler will process.
 */
@FunctionalInterface
public interface EventHandler<E> {

    /**
     * Handles a list of events. Implementations of this method should define the logic
     * for processing the events provided in the list. Each event in the list is an
     * instance of {@link Event} parameterized by the type {@code <E>}, which represents
     * the data associated with the event.
     *
     * @param events A list of events to be handled. Each event contains data of type {@code <E>}.
     */
    void handle(List<Event<E>> events);
}

