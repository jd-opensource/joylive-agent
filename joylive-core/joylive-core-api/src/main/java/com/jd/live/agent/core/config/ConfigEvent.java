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
package com.jd.live.agent.core.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * A configuration class that represents a single configuration setting.
 */
@Setter
@Getter
public class ConfigEvent {

    protected EventType type;

    /**
     * The name of the configuration setting.
     */
    protected String name;

    /**
     * The value of the configuration setting.
     */
    protected Object value;

    /**
     * A description of the configuration setting.
     */
    protected String description;

    protected String watcher;

    public ConfigEvent() {
    }

    @Builder
    public ConfigEvent(EventType type, String name, Object value, String description, String watcher) {
        this.type = type;
        this.name = name;
        this.value = value;
        this.description = description;
        this.watcher = watcher;
    }

    /**
     * An enumeration representing different types of events.
     */
    @Getter
    public enum EventType {

        /**
         * Represents an event to update all items.
         */
        UPDATE_ALL("update all"),

        /**
         * Represents an event to update a specific item.
         */
        UPDATE_ITEM("update item"),

        /**
         * Represents an event to delete a specific item.
         */
        DELETE_ITEM("delete item");

        private final String name;

        EventType(String name) {
            this.name = name;
        }

    }

}
