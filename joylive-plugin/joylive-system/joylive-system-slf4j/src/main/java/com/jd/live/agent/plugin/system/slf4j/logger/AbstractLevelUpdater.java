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

import com.jd.live.agent.core.util.KeyValue;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An abstract class that provides a common implementation for updating the logging level for loggers.
 */
public abstract class AbstractLevelUpdater implements LevelUpdater {

    protected static final Map<Class<?>, KeyValue<Method, Class<?>>> METHODS = new ConcurrentHashMap<>();
    protected static final Map<String, Optional<Object>> LEVELS = new ConcurrentHashMap<>();
    protected static final String METHOD_SET_LEVEL = "setLevel";

    @Override
    public void update(Logger logger, String loggerName, String level) throws Throwable {
        KeyValue<Method, Class<?>> keyValue = METHODS.computeIfAbsent(logger.getClass(), this::findMethod);
        Method method = keyValue.getKey();
        Class<?> levelType = keyValue.getValue();
        if (method != null) {
            Optional<Object> optional = LEVELS.computeIfAbsent(level, k -> Optional.ofNullable(findLevel(levelType, k)));
            if (optional.isPresent()) {
                method.invoke(logger, optional.get());
            }
        }
    }

    /**
     * Finds the specified level field in the given class.
     *
     * @param type  The class to search for the level field.
     * @param level The name of the level field to find.
     * @return The value of the level field if found, otherwise null.
     */
    protected Object findLevel(Class<?> type, String level) {
        try {
            Field field = type.getDeclaredField(level);
            if (Modifier.isStatic(field.getModifiers())
                    && Modifier.isFinal(field.getModifiers())
                    && Modifier.isPublic(field.getModifiers())
                    && field.getType() == type) {
                field.setAccessible(true);
                return field.get(null);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Returns the "setLevel" method and its corresponding level type for the specified logger class.
     *
     * @param type The logger class.
     * @return A KeyValue object containing the "setLevel" method and its corresponding level type.
     */
    protected KeyValue<Method, Class<?>> findMethod(Class<?> type) {
        Method[] methods = type.getDeclaredMethods();
        for (Method method : methods) {
            KeyValue<Method, Class<?>> keyValue = findMethod(method);
            if (keyValue != null) return keyValue;
        }
        return new KeyValue<>(null, null);
    }

    /**
     * Finds the "setLevel" method and its corresponding level type for the specified method.
     *
     * @param method The method to check if it is the "setLevel" method.
     * @return A KeyValue object containing the "setLevel" method and its corresponding level type, or null if the method is not the "setLevel" method.
     */
    protected KeyValue<Method, Class<?>> findMethod(Method method) {
        if (method.getName().equals(METHOD_SET_LEVEL)) {
            Parameter[] parameters = method.getParameters();
            if (parameters.length == 1) {
                method.setAccessible(true);
                Class<?> levelType = parameters[0].getType();
                return new KeyValue<>(method, levelType);
            }
        }
        return null;
    }
}
