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
package com.jd.live.agent.governance.db.mq;

import lombok.Getter;

/**
 * Defines the roles in a message queue system.
 *
 * <p>Possible values:
 * <ul>
 *   <li>{@code PRODUCER} - Message sender role</li>
 *   <li>{@code CONSUMER} - Message receiver role</li>
 * </ul>
 *
 * @see #name Role identifier (lowercase)
 */
public enum MQClientRole {

    PRODUCER("producer"),

    CONSUMER("consumer");

    @Getter
    private final String name;

    MQClientRole(String name) {
        this.name = name;
    }
}
