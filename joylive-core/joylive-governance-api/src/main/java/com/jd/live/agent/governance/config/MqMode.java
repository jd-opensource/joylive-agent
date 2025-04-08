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
package com.jd.live.agent.governance.config;

/**
 * MQ message consumption modes.
 * <p>
 * Defines different isolation strategies for message queue consumers:
 *
 * <ul>
 *   <li>{@link #ISOLATION_CLUSTER} - Consumer isolation at cluster level</li>
 *   <li>{@link #ISOLATION_TOPIC} - Consumer isolation at topic level</li>
 *   <li>{@link #SHARED} - No isolation, shared consumption</li>
 * </ul>
 */
public enum MqMode {
    ISOLATION_CLUSTER,
    ISOLATION_TOPIC,
    SHARED,
}
