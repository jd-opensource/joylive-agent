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
package com.jd.live.agent.governance.service.config;

/**
 * Interface for a configuration client API that supports connection management.
 * <p>
 * Extends {@link AutoCloseable} to ensure proper resource cleanup.
 */
public interface ConfigClientApi extends AutoCloseable {

    /**
     * Connects to the Nacos server using the specified configuration.
     *
     * @throws Exception If there is an error connecting to the config server.
     */
    void connect() throws Exception;

    @Override
    void close() throws Exception;
}
