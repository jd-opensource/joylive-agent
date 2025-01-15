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
package com.jd.live.agent.plugin.system.slf4j.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.subscription.config.ConfigCenter;
import com.jd.live.agent.governance.subscription.config.ConfigEvent;
import com.jd.live.agent.governance.subscription.config.ConfigEvent.EventType;
import com.jd.live.agent.governance.subscription.config.Configurator;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.config.ConfigCenterConfig;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.system.slf4j.logger.LevelUpdater;
import com.jd.live.agent.plugin.system.slf4j.logger.LevelUpdaterFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.core.util.StringUtils.SEMICOLON_COMMA_LINE;
import static com.jd.live.agent.core.util.StringUtils.splitMap;

/**
 * An interceptor class that listens for changes in logger levels and updates them accordingly.
 */
public class LoggerFactoryInterceptor extends InterceptorAdaptor {
    private static final Logger logger = LoggerFactory.getLogger(LoggerFactoryInterceptor.class);

    private static final String DEFAULT_LOGGER_KEY = "logger.level";

    private static final String KEY_LOGGER = "logger.key";

    private final Configurator configurator;

    private final ConfigCenterConfig config;

    private final Map<String, LoggerCache> loggerCaches = new ConcurrentHashMap<>();

    private Map<String, String> configs;

    public LoggerFactoryInterceptor(ConfigCenter configCenter, GovernanceConfig governanceConfig) {
        this.config = governanceConfig.getConfigCenterConfig();
        this.configurator = configCenter.getConfigurator();
        if (configurator != null) {
            String key = config.getOrDefault(KEY_LOGGER, DEFAULT_LOGGER_KEY);
            configurator.addListener(key, this::onUpdate);
        }
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        String name = mc.getArgument(0);
        loggerCaches.put(name, getCache(name, mc.getResult()));
    }

    /**
     * Called when a configuration change event occurs.
     *
     * @param event The configuration change event.
     * @return true if the logger levels were updated, false otherwise.
     */
    private boolean onUpdate(ConfigEvent event) {
        Object value = event.getValue();
        int counter = 0;
        if (event.getType() == EventType.DELETE
                || event.getType() == EventType.UPDATE && value == null) {
            counter += onDelete(null, configs);
            configs = null;
        } else if (event.getType() == EventType.UPDATE) {
            Map<String, String> newConfigs = new HashMap<>();
            splitMap(value.toString(), SEMICOLON_COMMA_LINE, true, (k, v) -> {
                if (v != null && !v.isEmpty()) {
                    newConfigs.put(k, v.toUpperCase());
                }
                return true;
            });
            counter += onUpdate(newConfigs, configs);
            counter += onDelete(newConfigs, configs);
            configs = newConfigs;
        }
        return counter > 0;
    }

    /**
     * Updates the logger levels based on the new and old configurations.
     *
     * @param newConfigs The new configuration map.
     * @param oldConfigs The old configuration map.
     * @return The number of logger levels updated.
     */
    private int onUpdate(Map<String, String> newConfigs, Map<String, String> oldConfigs) {
        int counter = 0;
        for (Map.Entry<String, String> entry : newConfigs.entrySet()) {
            String name = entry.getKey();
            String newLevel = entry.getValue();
            String oldLevel = oldConfigs == null ? null : oldConfigs.get(name);
            if (!newLevel.equals(oldLevel)) {
                LoggerCache cache = getCache(name);
                try {
                    if (update(cache, newLevel)) {
                        logger.info("Success updating logger level, " + name + "=" + newLevel);
                        counter++;
                    }
                } catch (Throwable e) {
                    logger.info("Failed to update logger level, " + name + "=" + newLevel + ", cased by " + e.getMessage());
                }
            }
        }
        return counter;
    }

    /**
     * Deletes the logger levels that are no longer present in the new configuration.
     *
     * @param newConfigs The new configuration map.
     * @param oldConfigs The old configuration map.
     * @return The number of logger levels deleted.
     */
    private int onDelete(Map<String, String> newConfigs, Map<String, String> oldConfigs) {
        int counter = 0;
        if (oldConfigs != null) {
            for (Map.Entry<String, String> entry : oldConfigs.entrySet()) {
                String name = entry.getKey();
                if (newConfigs == null || !newConfigs.containsKey(name)) {
                    LoggerCache cache = loggerCaches.get(name);
                    if (cache != null) {
                        try {
                            if (update(cache, cache.level)) {
                                logger.info("Success updating logger level, " + name + "=" + cache.level);
                                counter++;
                            }
                        } catch (Throwable e) {
                            logger.info("Failed to update logger level, " + name + "=" + cache.level + ", cased by " + e.getMessage());
                        }
                    }
                }
            }
        }
        return counter;
    }

    /**
     * Updates the logging level of the specified logger.
     *
     * @param level The new logging level.
     * @return true if the logger level was successfully updated, false otherwise.
     * @throws Throwable If an exception occurs during the update process.
     */
    private boolean update(LoggerCache cache, String level) throws Throwable {
        if (cache.updater != null && level != null && !level.isEmpty()) {
            cache.update(level);
            return true;
        }
        return false;
    }

    /**
     * Returns a logger instance for the specified name.
     *
     * @param name The name of the logger.
     * @return A logger instance for the specified name.
     */
    private LoggerCache getCache(String name) {
        return getCache(name, org.slf4j.LoggerFactory.getLogger(name));
    }

    /**
     * Returns a logger cache instance for the specified name and logger.
     *
     * @param name   The name of the logger.
     * @param logger The logger instance.
     * @return A logger cache instance for the specified name and logger.
     */
    private LoggerCache getCache(String name, org.slf4j.Logger logger) {
        LevelUpdater updater = LevelUpdaterFactory.getLevelUpdater(logger);
        String level = updater == null ? null : updater.getLevel(logger);
        return new LoggerCache(name, logger, updater, level);
    }


    /**
     * A private static inner class that caches logger instances and their corresponding levels.
     */
    private static class LoggerCache {

        /**
         * The name of the logger.
         */
        private final String name;

        /**
         * The logger instance.
         */
        private final org.slf4j.Logger logger;

        /**
         * The level updater responsible for updating the logger's level.
         */
        private final LevelUpdater updater;

        /**
         * The current level of the logger.
         */
        private final String level;

        /**
         * Constructs a new LoggerCache instance with the specified name, logger, level updater, and level.
         *
         * @param name    The name of the logger.
         * @param logger  The logger instance.
         * @param updater The level updater responsible for updating the logger's level.
         * @param level   The current level of the logger.
         */
        LoggerCache(String name, org.slf4j.Logger logger, LevelUpdater updater, String level) {
            this.name = name;
            this.logger = logger;
            this.updater = updater;
            this.level = level;
        }

        /**
         * Updates the level of the cached logger instance.
         *
         * @param level The new level to set.
         * @throws Throwable If an error occurs while updating the logger's level.
         */
        public void update(String level) throws Throwable {
            updater.update(logger, name, level);
        }
    }


}
