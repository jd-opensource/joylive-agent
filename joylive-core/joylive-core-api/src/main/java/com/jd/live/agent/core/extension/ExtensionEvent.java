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
package com.jd.live.agent.core.extension;

import lombok.Getter;

/**
 * Represents an event that occurs related to an extension.
 * It holds information about the type of event, the description of the extension,
 * an optional throwable if the event represents an error, and an optional instance
 * of the extension if the event is about its creation.
 *
 * @since 1.0.0
 */
@Getter
public class ExtensionEvent {

    /**
     * The type of event that occurred.
     */
    private final EventType type;

    /**
     * The description of the extension related to this event.
     */
    private final ExtensionDesc<?> desc;

    /**
     * The throwable if this event represents an error or failure.
     */
    private final Throwable throwable;

    /**
     * The instance of the extension if this event is about its creation.
     */
    private final Object instance;

    /**
     * Constructs a new ExtensionEvent representing a failure.
     *
     * @param desc      The description of the extension.
     * @param throwable The cause of the failure.
     */
    public ExtensionEvent(ExtensionDesc<?> desc, Throwable throwable) {
        this.type = EventType.FAILED;
        this.desc = desc;
        this.throwable = throwable;
        this.instance = null;
    }

    /**
     * Constructs a new ExtensionEvent representing the creation of an extension.
     *
     * @param desc  The description of the extension.
     * @param instance The instance of the created extension.
     */
    public ExtensionEvent(ExtensionDesc<?> desc, Object instance) {
        this.type = EventType.CREATED;
        this.desc = desc;
        this.throwable = null;
        this.instance = instance;
    }

    /**
     * The possible types of extension events.
     */
    public enum EventType {

        /**
         * Represents the creation of an extension.
         */
        CREATED,

        /**
         * Represents a failure in an extension operation.
         */
        FAILED

    }
}

