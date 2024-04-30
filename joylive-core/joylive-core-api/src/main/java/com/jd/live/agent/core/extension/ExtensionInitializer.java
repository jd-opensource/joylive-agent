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
 * The {@code ExtensionInitializer} interface defines a contract for classes that are responsible
 * for initializing extensions. Implementations of this interface perform the necessary setup
 * operations to prepare an extension for use within the system.
 */
public interface ExtensionInitializer {

    /**
     * Performs the initialization process for an extension. This method is called when an
     * extension needs to be set up, which may include tasks such as loading resources,
     * configuring settings, or setting up required dependencies.
     */
    void initialize();
}