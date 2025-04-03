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

import com.jd.live.agent.plugin.system.slf4j.logger.AbstractLevelUpdater;

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

    private static final String TYPE_LEVEL = "org.apache.logging.log4j.Level";

    private static final Map<String, Optional<Class<?>>> TYPES = new ConcurrentHashMap<>();

    @Override
    public boolean support(Object logger) {
        return logger.getClass().getName().startsWith("org.apache.logging.log4j");
    }

    @Override
    protected void invoke(Method setter, Object logger, String loggerName, String level, Object levelObj) throws Throwable {
        setter.invoke(null, loggerName, level);
    }

    @Override
    protected MethodCache findMethod(Class<?> type) {
        Method getter = getGetter(type.getDeclaredMethods());
        Class<?> configuratorType = getType(TYPE_CONFIGURATOR, type.getClassLoader());
        Method setter = getMethod(configuratorType.getDeclaredMethods(), this::isSetter);
        Class<?> levelType = null;
        if (getter != null) {
            levelType = getType(TYPE_LEVEL, type.getClassLoader());
        }
        return new MethodCache(getter, setter, levelType, getLevels(levelType));
    }

    /**
     * Checks if the specified method is a setter method for the level property with two String parameters.
     *
     * @param method The method to check.
     * @return true if the method is a setter method for the level property with two String parameters, false otherwise.
     */
    private boolean isSetter(Method method) {
        if (method.getName().equals(METHOD_SET_LEVEL)) {
            Parameter[] parameters = method.getParameters();
            if (parameters.length == 2
                    && parameters[0].getType() == String.class
                    && parameters[1].getType() == String.class) {
                method.setAccessible(true);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the Class object representing the specified type, loaded using the given class loader.
     *
     * @param name The name of the type to load.
     * @param classLoader The class loader to use for loading the type.
     * @return The Class object representing the specified type, or null if the type could not be loaded.
     */
    private Class<?> getType(String name, ClassLoader classLoader) {
        return TYPES.computeIfAbsent(name, n -> {
            try {
                return Optional.of(Class.forName(n, true, classLoader));
            } catch (Throwable e) {
                return Optional.empty();
            }
        }).orElse(null);
    }
}
