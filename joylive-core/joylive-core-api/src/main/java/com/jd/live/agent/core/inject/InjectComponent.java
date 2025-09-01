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

import lombok.Getter;

import java.util.Map;

/**
 * Provides component injection by key and type.
 */
public interface InjectComponent {

    /**
     * Retrieves a component by key and type.
     *
     * @param <T>  the component type
     * @param key  the component key
     * @param type the component class
     * @return the component instance
     */
    <T> T getComponent(String key, Class<T> type);

    abstract class AbstractInjectComponent implements InjectComponent {

        /**
         * A map of component names to their instances. These components are available for injection
         * and can be dynamically added to the injection source.
         */
        @Getter
        protected Map<String, Object> components;

        public AbstractInjectComponent(Map<String, Object> components) {
            this.components = components;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getComponent(String key, Class<T> type) {
            Object result = components == null ? null : components.get(key);
            return !type.isInstance(result) ? null : (T) result;
        }

    }
}
