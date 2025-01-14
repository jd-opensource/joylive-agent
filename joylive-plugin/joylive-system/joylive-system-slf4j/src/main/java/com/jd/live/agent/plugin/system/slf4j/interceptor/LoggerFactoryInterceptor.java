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
import com.jd.live.agent.core.config.ConfigCenter;
import com.jd.live.agent.core.config.ConfigEvent;
import com.jd.live.agent.core.config.ConfigEvent.EventType;
import com.jd.live.agent.core.config.Configurator;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.plugin.system.slf4j.logger.LevelUpdater;
import com.jd.live.agent.plugin.system.slf4j.logger.LevelUpdaterFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.core.util.StringUtils.SEMICOLON_COMMA_LINE;
import static com.jd.live.agent.core.util.StringUtils.splitMap;

/**
 * An interceptor class that listens for changes in logger levels and updates them accordingly.
 */
public class LoggerFactoryInterceptor extends InterceptorAdaptor {
    private static final Logger logger = LoggerFactory.getLogger(LoggerFactoryInterceptor.class);

    private final Configurator configurator;

    private static final Map<String, Optional<org.slf4j.Logger>> LOGGERS = new ConcurrentHashMap<>();

    public LoggerFactoryInterceptor(ConfigCenter configCenter) {
        this.configurator = configCenter.getConfigurator();
        if (configurator != null) {
            configurator.addListener("logger.level", this::onUpdate);
        }
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        LOGGERS.put(mc.getArgument(0), mc.getResult());
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
        if (event.getType() == EventType.UPDATE && value != null) {
            Map<String, String> levels = splitMap(value.toString(), SEMICOLON_COMMA_LINE);
            for (Map.Entry<String, String> entry : levels.entrySet()) {
                String name = entry.getKey();
                String level = entry.getValue();
                try {
                    if (update(name, level)) {
                        logger.info("Success updating logger level, " + name + "=" + level);
                        counter++;
                    }
                } catch (Throwable e) {
                    logger.info("Failed to update logger level, " + name + "=" + level + ", cased by " + e.getMessage());
                }
            }
        }
        return counter > 0;
    }

    /**
     * Updates the logging level of the specified logger.
     *
     * @param name  The name of the logger to update.
     * @param level The new logging level.
     * @return true if the logger level was successfully updated, false otherwise.
     * @throws Throwable If an exception occurs during the update process.
     */
    private boolean update(String name, String level) throws Throwable {
        Optional<org.slf4j.Logger> optional = LOGGERS.computeIfAbsent(name, n -> Optional.ofNullable(getLogger(n)));
        if (optional.isPresent()) {
            org.slf4j.Logger logging = optional.get();
            LevelUpdater updater = LevelUpdaterFactory.getLevelUpdater(logging);
            if (updater != null) {
                if (level != null && !level.isEmpty()) {
                    level = level.toUpperCase();
                    updater.update(logging, name, level);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a logger instance for the specified name.
     *
     * @param name The name of the logger.
     * @return A logger instance for the specified name.
     */
    private org.slf4j.Logger getLogger(String name) {
        return org.slf4j.LoggerFactory.getLogger(name);
    }

}
