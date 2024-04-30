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
package com.jd.live.agent.governance.invoke;

import lombok.Getter;

/**
 * Represents an action to be taken by a unit with a specific type and optional message.
 * This class is immutable and provides factory methods for creating instances of UnitAction.
 */
@Getter
public class UnitAction {

    /**
     * The type of action this UnitAction represents.
     */
    private final UnitActionType type;

    /**
     * An optional message associated with the action.
     */
    private final String message;

    /**
     * Creates a new UnitAction with the specified type and no message.
     *
     * @param type the type of action
     */
    public UnitAction(UnitActionType type) {
        this(type, null);
    }

    /**
     * Creates a new UnitAction with the specified type and message.
     *
     * @param type    the type of action
     * @param message the message associated with the action, may be null
     */
    public UnitAction(UnitActionType type, String message) {
        this.type = type;
        this.message = message;
    }

    /**
     * Factory method to create a UnitAction of type FORWARD with no message.
     *
     * @return a UnitAction with type FORWARD
     */
    public static UnitAction forward() {
        return new UnitAction(UnitActionType.FORWARD);
    }

    /**
     * Factory method to create a UnitAction of type REJECT with a message.
     *
     * @param message the message associated with the rejection
     * @return a UnitAction with type REJECT and the provided message
     */
    public static UnitAction reject(String message) {
        return new UnitAction(UnitActionType.REJECT, message);
    }

    /**
     * Enumerates the possible types of actions that a unit can take.
     */
    public enum UnitActionType {
        /**
         * Proceed with the action.
         */
        FORWARD,

        /**
         * Reject the action with an optional message.
         */
        REJECT,

        /**
         * Reject the action and indicate that it was escaped.
         */
        REJECT_ESCAPED,

        /**
         * Failover to an alternative action.
         */
        FAILOVER,

        /**
         * Failover to a central action.
         */
        FAILOVER_CENTER
    }
}

