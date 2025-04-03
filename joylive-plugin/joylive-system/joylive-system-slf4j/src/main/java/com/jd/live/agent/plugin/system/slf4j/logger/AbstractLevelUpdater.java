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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * An abstract class that provides a common implementation for updating the logging level for loggers.
 */
public abstract class AbstractLevelUpdater implements LevelUpdater {

    protected static final Map<Class<?>, MethodCache> METHODS = new ConcurrentHashMap<>();
    protected static final String METHOD_SET_LEVEL = "setLevel";
    protected static final String METHOD_GET_LEVEL = "getLevel";

    @Override
    public void update(Object logger, String loggerName, String level) throws Throwable {
        MethodCache cache = METHODS.computeIfAbsent(logger.getClass(), this::findMethod);
        Method setter = cache.getSetter();
        if (setter != null) {
            Object levelObj = cache.getLevel(level);
            if (levelObj != null) {
                invoke(setter, logger, loggerName, level, levelObj);
            }
        }
    }

    @Override
    public String getLevel(Object logger) {
        MethodCache cache = METHODS.computeIfAbsent(logger.getClass(), this::findMethod);
        Method method = cache.getGetter();
        if (method != null) {
            try {
                return method.invoke(logger).toString();
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    /**
     * Invokes the specified setter method on the given logger instance, passing the provided level object as an argument.
     *
     * @param setter The setter method to invoke.
     * @param logger The logger instance on which to invoke the setter method.
     * @param loggerName The name of the logger instance.
     * @param level The level to set on the logger instance.
     * @param levelObj The level object to pass as an argument to the setter method.
     * @throws Throwable If an exception occurs during the invocation of the setter method.
     */
    protected void invoke(Method setter, Object logger, String loggerName, String level, Object levelObj) throws Throwable {
        setter.invoke(logger, levelObj);
    }

    /**
     * Finds the getter and setter methods for the level property in the specified class.
     *
     * @param type The class to search for the methods.
     * @return A MethodCache instance containing the getter, setter, and type of the level property.
     */
    protected MethodCache findMethod(Class<?> type) {
        Method[] methods = type.getDeclaredMethods();
        Method getter = getGetter(methods);
        Method setter = getSetter(methods);
        Class<?> levelType = null;
        if (getter != null) {
            levelType = getter.getReturnType();
        } else if (setter != null) {
            levelType = setter.getParameters()[0].getType();
        }
        return new MethodCache(getter, setter, levelType, getLevels(levelType));
    }

    /**
     * Finds the setter method for the level property in the specified array of methods.
     *
     * @param methods The array of methods to search for the setter method.
     * @return The setter method for the level property, or null if not found.
     */
    protected Method getSetter(Method[] methods) {
        return getMethod(methods, method -> method.getName().equals(METHOD_SET_LEVEL) && method.getParameterCount() == 1);
    }

    /**
     * Finds the getter method for the level property in the specified array of methods.
     *
     * @param methods The array of methods to search for the getter method.
     * @return The getter method for the level property, or null if not found.
     */
    protected Method getGetter(Method[] methods) {
        return getMethod(methods, method -> method.getName().equals(METHOD_GET_LEVEL) && method.getParameterCount() == 0);
    }

    /**
     * Finds a method in the specified array of methods that satisfies the given predicate.
     *
     * @param methods   The array of methods to search for a matching method.
     * @param predicate The predicate to test each method against.
     * @return The first method that satisfies the predicate, or null if no matching method is found.
     */
    protected Method getMethod(Method[] methods, Predicate<Method> predicate) {
        for (Method method : methods) {
            if (predicate.test(method)) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    /**
     * Retrieves the static, final, and public fields of the specified class that are of the same type as the class itself.
     *
     * @param type The class to retrieve the fields from.
     * @return A map containing the names and values of the retrieved fields.
     */
    protected Map<String, Object> getLevels(Class<?> type) {
        Map<String, Object> levels = new HashMap<>();
        if (type != null) {
            Field[] fields = type.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())
                        && Modifier.isFinal(field.getModifiers())
                        && Modifier.isPublic(field.getModifiers())
                        && field.getType() == type) {
                    try {
                        field.setAccessible(true);
                        levels.put(field.getName(), field.get(null));
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return levels;
    }

    /**
     * A private static inner class that caches getter and setter methods for a specific property.
     */
    protected static class MethodCache {

        /**
         * The getter method for the property.
         */
        private final Method getter;

        /**
         * The setter method for the property.
         */
        private final Method setter;

        /**
         * The type of the property.
         */
        private final Class<?> type;

        private final Map<String, Object> levels;

        public MethodCache(Method getter, Method setter, Class<?> type, Map<String, Object> levels) {
            this.getter = getter;
            this.setter = setter;
            this.type = type;
            this.levels = levels;
        }

        public Method getGetter() {
            return getter;
        }

        public Method getSetter() {
            return setter;
        }

        public Class<?> getType() {
            return type;
        }

        public Object getLevel(String level) {
            return levels.get(level);
        }
    }
}
