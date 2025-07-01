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
package com.jd.live.agent.core.util;

/**
 * Represents an executable operation that may throw an exception.
 * Implementations should define the operation in {@code execute()}.
 */
@FunctionalInterface
public interface Execution {
    /**
     * Performs the defined operation.
     *
     * @throws Exception if execution fails
     */
    void execute() throws Exception;
}

