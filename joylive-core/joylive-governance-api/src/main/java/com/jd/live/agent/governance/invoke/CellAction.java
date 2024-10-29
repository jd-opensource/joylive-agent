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
 * Represents an action to be taken by a cell with a specific type and an optional message.
 * This class is immutable and provides a constructor for creating instances of CellAction.
 */
@Getter
public class CellAction {

    /**
     * The type of action this CellAction represents.
     */
    private final CellActionType type;

    /**
     * An optional message associated with the action.
     */
    private final String message;

    public CellAction(CellActionType type) {
        this(type, null);
    }

    /**
     * Creates a new CellAction with the specified type and message.
     *
     * @param type the type of action
     * @param message the message associated with the action, may be null
     */
    public CellAction(CellActionType type, String message) {
        this.type = type;
        this.message = message;
    }

    /**
     * Enumerates the possible types of actions that a cell can take.
     */
    public enum CellActionType {
        /**
         * Proceed with the action.
         */
        FORWARD,

        /**
         * Failover to an alternative action or resource.
         */
        FAILOVER
    }
}

