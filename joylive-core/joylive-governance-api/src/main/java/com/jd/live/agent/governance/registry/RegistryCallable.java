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
package com.jd.live.agent.governance.registry;

import java.util.concurrent.Callable;

/**
 * Callable that provides access to a RegistryService.
 *
 * @param <T> the result type of the callable
 */
public interface RegistryCallable<T> extends Callable<T> {

    /**
     * Gets the associated registry service.
     *
     * @return the registry service instance
     */
    RegistryService getRegistry();

}
