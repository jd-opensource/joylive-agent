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
package com.jd.live.agent.bootstrap.logger;

/**
 * Interface for a bridge that provides access to a {@link Logger} instance. This is typically used in scenarios
 * where a logging framework abstraction is required, allowing for the underlying logging implementation to be
 * swapped or customized without changing the application code.
 *
 * @since 1.0.0
 */
public interface LoggerBridge {

    /**
     * Retrieves a {@link Logger} instance associated with the specified class. This method is typically used
     * to obtain a logger that is named after the class, which helps in categorizing and filtering log messages.
     *
     * @param clazz the class for which the logger is to be obtained.
     * @return a {@link Logger} instance associated with the specified class.
     */
    Logger getLogger(Class<?> clazz);
}

