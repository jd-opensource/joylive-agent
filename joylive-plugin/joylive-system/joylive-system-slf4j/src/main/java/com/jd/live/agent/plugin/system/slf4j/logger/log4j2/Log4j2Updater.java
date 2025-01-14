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
package com.jd.live.agent.plugin.system.slf4j.logger.log4j2;

import com.jd.live.agent.core.util.KeyValue;
import com.jd.live.agent.plugin.system.slf4j.logger.AbstractLevelUpdater;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class that updates the logging level for log4j loggers.
 */
public class Log4j2Updater extends AbstractLevelUpdater {

    private static final String TYPE_CONFIGURATOR = "org.apache.logging.log4j.core.config.Configurator";

    private static final Map<String, Optional<Class<?>>> TYPES = new ConcurrentHashMap<>();

    @Override
    public void update(Logger logger, String loggerName, String level) throws Throwable {
        Optional<Class<?>> optional = TYPES.computeIfAbsent(TYPE_CONFIGURATOR, n -> {
            try {
                return Optional.of(Class.forName(n, true, logger.getClass().getClassLoader()));
            } catch (Throwable e) {
                return Optional.empty();
            }
        });
        if (optional.isPresent()) {
            // org.apache.logging.log4j.core.config.Configurator.setLevel(java.lang.String, java.lang.String)
            KeyValue<Method, Class<?>> keyValue = METHODS.computeIfAbsent(optional.get(), this::findMethod);
            Method method = keyValue.getKey();
            if (method != null) {
                method.invoke(null, logger, level);
            }
        }
    }

    @Override
    public boolean support(Logger logger) {
        return logger.getClass().getName().startsWith("org.apache.logging.log4j");
    }

    @Override
    protected KeyValue<Method, Class<?>> findMethod(Method method) {
        if (method.getName().equals(METHOD_SET_LEVEL)) {
            Parameter[] parameters = method.getParameters();
            if (parameters.length == 2
                    && parameters[0].getType() == String.class
                    && parameters[1].getType() == String.class) {
                method.setAccessible(true);
                return new KeyValue<>(method, null);
            }
        }
        return null;
    }
}
