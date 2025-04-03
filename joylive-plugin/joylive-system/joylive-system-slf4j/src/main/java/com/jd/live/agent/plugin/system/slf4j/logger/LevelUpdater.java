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
package com.jd.live.agent.plugin.system.slf4j.logger;

/**
 * An interface for updating the logging level of a logger.
 */
public interface LevelUpdater {

    /**
     * Updates the logging level of the specified logger.
     *
     * @param logger     The logger to update.
     * @param loggerName The name of the logger.
     * @param level      The new logging level.
     */
    void update(Object logger, String loggerName, String level) throws Throwable;

    /**
     * Returns the level of the current instance.
     *
     * @return The level of the current instance.
     */
    String getLevel(Object logger);

    /**
     * Checks if this level updater supports the specified logger.
     *
     * @param logger The logger to check.
     * @return true if this level updater supports the specified logger, false otherwise.
     */
    boolean support(Object logger);
}

