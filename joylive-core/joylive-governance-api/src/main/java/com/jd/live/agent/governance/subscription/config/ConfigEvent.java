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
package com.jd.live.agent.governance.subscription.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * A class representing a configuration event.
 *
 * @since 1.0
 */
@Setter
@Getter
public class ConfigEvent {

    /**
     * The type of the configuration event.
     */
    protected EventType type;

    /**
     * The name of the configuration.
     */
    protected String name;

    /**
     * The value of the configuration.
     */
    protected Object value;

    /**
     * The version of the configuration.
     */
    protected long version;

    /**
     * Default constructor.
     */
    public ConfigEvent() {
    }

    /**
     * Constructor using the Builder pattern.
     *
     * @param type    The type of the configuration event.
     * @param name    The name of the configuration.
     * @param value   The value of the configuration.
     * @param version The version of the configuration.
     */
    @Builder
    public ConfigEvent(EventType type, String name, Object value, long version) {
        this.type = type;
        this.name = name;
        this.value = value;
        this.version = version;
    }

    /**
     * An enum representing the type of configuration event.
     */
    @Getter
    public enum EventType {

        /**
         * An update event.
         */
        UPDATE("update"),

        /**
         * A delete event.
         */
        DELETE("delete");

        /**
         * The name of the event type.
         */
        private final String name;

        /**
         * Constructor for the EventType enum.
         *
         * @param name The name of the event type.
         */
        EventType(String name) {
            this.name = name;
        }

    }
}

