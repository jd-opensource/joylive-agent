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
package com.jd.live.agent.core.inject;

/**
 * Defines the contract for injection mechanisms that facilitate the transfer or injection of data
 * or dependencies from one object to another.
 */
public interface Injection {

    /**
     * Injects data or dependencies from the source object into the target object.
     *
     * @param source The object from which data or dependencies are sourced.
     * @param target The object into which data or dependencies are injected.
     */
    void inject(Object source, Object target);

    /**
     * Functional interface for injectors that focus on injecting data or dependencies into a single target object.
     */
    @FunctionalInterface
    interface Injector {

        /**
         * Injects data or dependencies into the target object.
         *
         * @param target The object into which data or dependencies are injected.
         */
        void inject(Object target);
    }
}

