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

/**
 * The {@code ExtensionListener} interface defines a contract for classes that wish to listen to
 * extension events. An extension event can represent various actions such as the loading,
 * unloading, or updating of an extension within the system.
 */
@FunctionalInterface
public interface ExtensionListener {

    /**
     * Invoked when an extension event occurs. This method is called by the extension system
     * to notify the listener about the event. The specific type of event is passed to the method,
     * allowing the listener to react accordingly.
     *
     * @param event the extension event that has occurred. The event object contains information
     *              about the type of event and the extension(s) involved.
     */
    void onEvent(ExtensionEvent event);
}

