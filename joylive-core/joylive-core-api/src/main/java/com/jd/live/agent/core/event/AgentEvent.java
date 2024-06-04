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
     * Creates an AgentEvent indicating that the agent is ready.
     *
     * @param message the message describing the event
     * @return a new AgentEvent of type AGENT_READY
     */
    public static AgentEvent onAgentReady(String message) {
        return new AgentEvent(EventType.AGENT_READY, message);
    }

    /**
     * Creates an AgentEvent indicating that the agent has encountered a failure.
     *
     * @param message   the message describing the event
     * @param throwable the throwable associated with the failure
     * @return a new AgentEvent of type AGENT_FAILURE
     */
    public static AgentEvent onAgentFailure(String message, Throwable throwable) {
        return new AgentEvent(EventType.AGENT_FAILURE, message, throwable);
    }

    /**
     * Creates an AgentEvent indicating that the agent service is ready.
     *
     * @param message the message describing the event
     * @return a new AgentEvent of type AGENT_SERVICE_READY
     */
    public static AgentEvent onAgentServiceReady(String message) {
        return new AgentEvent(EventType.AGENT_SERVICE_READY, message);
    }

    /**
     * Creates an AgentEvent indicating that the agent enhancement is ready.
     *
     * @param message the message describing the event
     * @return a new AgentEvent of type AGENT_ENHANCE_READY
     */
    public static AgentEvent onAgentEnhanceReady(String message) {
        return new AgentEvent(EventType.AGENT_ENHANCE_READY, message);
    }

    /**
     * Creates an AgentEvent indicating that the agent enhancement has encountered a failure.
     *
     * @param message   the message describing the event
     * @param throwable the throwable associated with the failure
     * @return a new AgentEvent of type AGENT_ENHANCE_FAILURE
     */
    public static AgentEvent onAgentEnhanceFailure(String message, Throwable throwable) {
        return new AgentEvent(EventType.AGENT_ENHANCE_FAILURE, message, throwable);
    }

    /**
     * Creates an AgentEvent indicating that the agent service policy is ready.
     *
     * @param message the message describing the event
     * @return a new AgentEvent of type AGENT_SERVICE_POLICY_READY
     */
    public static AgentEvent onServicePolicyReady(String message) {
        return new AgentEvent(EventType.AGENT_SERVICE_POLICY_READY, message);
    }

    /**
     * Creates an AgentEvent indicating that the agent service policy has encountered a failure.
     *
     * @param message   the message describing the event
     * @param throwable the throwable associated with the failure
     * @return a new AgentEvent of type AGENT_SERVICE_POLICY_FAILURE
     */
    public static AgentEvent onServicePolicyFailure(String message, Throwable throwable) {
        return new AgentEvent(EventType.AGENT_SERVICE_POLICY_FAILURE, message, throwable);
    }

    /**
     * Creates an AgentEvent indicating that the application is ready.
     *
     * @param message the message describing the event
     * @return a new AgentEvent of type APPLICATION_READY
     */
    public static AgentEvent onApplicationReady(String message) {
        return new AgentEvent(EventType.APPLICATION_READY, message);
    }

    /**
     * Creates an AgentEvent indicating that the application is stopping.
     *
     * @param message the message describing the event
     * @return a new AgentEvent of type APPLICATION_STOP
     */
    public static AgentEvent onApplicationStop(String message) {
        return new AgentEvent(EventType.APPLICATION_STOP, message);
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
        AGENT_READY,

        /**
         * Indicates that the agent failed to start.
         */
        AGENT_FAILURE,

        /**
         * Indicates that all agent's services have started.
         */
        AGENT_SERVICE_READY,

        /**
         * Indicates a successful enhancement or modification performed by the agent.
         */
        AGENT_ENHANCE_READY,

        /**
         * Indicates a failure in an enhancement or modification attempt by the agent.
         */
        AGENT_ENHANCE_FAILURE,

        /**
         * Indicates a failure in the initialization of an agent's policy or configuration.
         */
        AGENT_SERVICE_POLICY_FAILURE,

        /**
         * Indicates a successful initialization of an agent's policy or configuration.
         */
        AGENT_SERVICE_POLICY_READY,

        /**
         * Indicates a successful initialization of application.
         */
        APPLICATION_READY,

        /**
         * Indicates that the application is stopping.
         */
        APPLICATION_STOP

    }

}

