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

import com.jd.live.agent.plugin.system.slf4j.logger.jul.JulUpdater;
import com.jd.live.agent.plugin.system.slf4j.logger.log4j.Log4jUpdater;
import com.jd.live.agent.plugin.system.slf4j.logger.log4j2.Log4j2Updater;
import com.jd.live.agent.plugin.system.slf4j.logger.logback.LogbackUpdater;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A factory class for creating level updaters for different logging frameworks.
 */
public abstract class LevelUpdaterFactory {

    /**
     * A list of level updaters for different logging frameworks.
     */
    private static final List<LevelUpdater> UPDATERS = Arrays.asList(
            new LogbackUpdater(), new Log4jUpdater(), new JulUpdater(), new Log4j2Updater());

    /**
     * A map that stores the level updater for each logger class.
     */
    private static final Map<Class<?>, Optional<LevelUpdater>> TYPE_UPDATERS = new ConcurrentHashMap<>();

    /**
     * Returns the level updater for the specified logger.
     *
     * @param logger The logger to get the level updater for.
     * @return The level updater for the specified logger, or null if no updater is found.
     */
    public static LevelUpdater getLevelUpdater(org.slf4j.Logger logger) {
        return TYPE_UPDATERS.computeIfAbsent(logger.getClass(), k -> {
            LevelUpdater levelUpdater = getUpdater(logger);
            return Optional.ofNullable(levelUpdater);
        }).orElse(null);
    }

    /**
     * Returns the level updater that supports the specified logger.
     *
     * @param logger The logger to find the level updater for.
     * @return The level updater that supports the specified logger, or null if no updater is found.
     */
    private static LevelUpdater getUpdater(Logger logger) {
        for (LevelUpdater levelUpdater : UPDATERS) {
            if (levelUpdater.support(logger)) {
                return levelUpdater;
            }
        }
        return null;
    }

}
