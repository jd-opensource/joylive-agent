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

/**
 * Represents a subscription to events of type {@code T} on a specific topic.
 *
 * @param <T> the type of events handled by this subscription
 */
public interface Subscription<T> extends EventHandler<T> {

    /**
     * Returns the topic this subscription is listening to.
     * @return the subscription topic
     */
    String getTopic();

}
