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

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a generic event that can be used in various contexts such as messaging,
 * event handling, or logging. This class encapsulates common attributes of an event,
 * including an instance ID, IP address, topic, timestamp, and event-specific data.
 *
 * @param <E> The type of the data associated with this event.
 */
@Getter
@Setter
public class Event<E> {

    /**
     * The unique identifier of the instance or component that generated the event.
     */
    private String instanceId;

    /**
     * The IP address of the source that generated the event.
     */
    private String ip;

    /**
     * The topic associated with this event, which can be used for routing or categorization.
     */
    private String topic;

    /**
     * The timestamp when the event was generated, typically represented as milliseconds since epoch.
     */
    private long time;

    /**
     * The data associated with this event, which can be of any type.
     */
    private E data;

    /**
     * Default constructor for creating an empty event.
     */
    public Event() {
    }

    /**
     * Constructs an event with the specified data.
     *
     * @param data The data associated with this event.
     */
    public Event(E data) {
        this.data = data;
    }

    /**
     * Clears all fields of this event for object reuse.
     * Called after event processing is complete.
     */
    public void clear() {
        this.instanceId = null;
        this.ip = null;
        this.topic = null;
        this.time = 0;
        this.data = null;
    }
}