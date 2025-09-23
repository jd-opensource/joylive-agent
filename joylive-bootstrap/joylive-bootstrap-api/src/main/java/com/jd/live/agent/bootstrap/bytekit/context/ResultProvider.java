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
package com.jd.live.agent.bootstrap.bytekit.context;

import java.util.function.BiConsumer;

/**
 * Provides access to method execution results.
 */
public interface ResultProvider {

    /**
     * Handles the method execution result by invoking the consumer with response and error.
     *
     * @param consumer the consumer to handle the result, accepting response object and throwable
     */
    void handle(BiConsumer<Object, Throwable> consumer);

}