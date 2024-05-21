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

/**
 * Represents an event related to an agent's lifecycle or actions. This class encapsulates
 * information about different types of agent events, such as start success, failure, service
 * start/stop, enhancement success/failure, and policy initialization success/failure.
 * Each event includes a type, a message, and an optional throwable if an error occurred.
 * This event structure is useful for logging, monitoring, or handling agent state changes
 * and actions within an application or system.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Getter
public class AgentEvent {

    /**
     * The type of the event, indicating what kind of event occurred.
     */
    private final EventType type;

    /**
     * A message associated with the event, providing additional details.
     */
    private final String message;

    /**
     * An optional throwable associated with the event, if an error occurred.
     */
    private final Throwable throwable;

    /**
     * Constructs an AgentEvent with the specified type and message, without an associated throwable.
     *
     * @param type    The type of the event.
     * @param message The message providing additional details about the event.
     */
    public AgentEvent(EventType type, String message) {
        this(type, message, null);
    }

    /**
     * Constructs an AgentEvent with the specified type, message, and throwable.
     *
     * @param type      The type of the event.
     * @param message   The message providing additional details about the event.
     * @param throwable An optional throwable if an error occurred.
     */
    public AgentEvent(EventType type, String message, Throwable throwable) {
        this.type = type;
        this.message = message;
        this.throwable = throwable;
    }

    /**
     * Enumerates the types of events related to an agent's lifecycle and operations.
     * These events can signify various states or transitions, such as starting, stopping,
     * success, or failure of an agent or its services.
     */
    public enum EventType {
        /**
         * Indicates that the agent has successfully started.
         */
        AGENT_START_SUCCESS,

        /**
         * Indicates that the agent failed to start.
         */
        AGENT_START_FAILURE,

        /**
         * Indicates that an agent's service has started.
         */
        AGENT_SERVICE_START,

        /**
         * Indicates that an agent's service has stopped.
         */
        AGENT_SERVICE_STOP,

        /**
         * Indicates that all agent's services have started.
         */
        AGENT_SERVICES_START,

        /**
         * Indicates a successful enhancement or modification performed by the agent.
         */
        AGENT_ENHANCE_SUCCESS,

        /**
         * Indicates a failure in an enhancement or modification attempt by the agent.
         */
        AGENT_ENHANCE_FAILURE,

        /**
         * Indicates a failure in the initialization of an agent's policy or configuration.
         */
        AGENT_POLICY_INITIALIZE_FAILURE,

        /**
         * Indicates a successful initialization of an agent's policy or configuration.
         */
        AGENT_POLICY_INITIALIZE_SUCCESS,
    }


}

