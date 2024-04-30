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
package com.jd.live.agent.core.util.type;

import com.jd.live.agent.bootstrap.exception.ReflectException;
import lombok.Getter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Manages constructors of a specified class, providing functionalities to access and instantiate objects.
 */
public class ConstructorList {
    /**
     * The class type for which the constructors are managed.
     */
    protected Class<?> type;
    /**
     * A map of single-argument public constructors, keyed by their parameter types.
     */
    protected Map<Class<?>, Constructor<?>> singleConstructors = new HashMap<>(3);
    /**
     * The default public constructor, if available.
     */
    @Getter
    protected Constructor<?> defaultConstructor;
    /**
     * The default single-argument constructor, if available.
     */
    @Getter
    protected Constructor<?> defaultSingleConstructor;
    /**
     * The constructor with the fewest parameters.
     */
    @Getter
    protected Constructor<?> minimumConstructor;
    /**
     * A list of all constructors of the class.
     */
    protected List<Constructor<?>> constructors = new LinkedList<>();

    /**
     * Constructs a new {@code ConstructorList} for the specified class.
     *
     * @param type The class type to manage constructors for.
     */
    public ConstructorList(Class<?> type) {
        this.type = type;
        // Check if the class is a public concrete class
        int modifiers = type.getModifiers();
        boolean concrete = !Modifier.isAbstract(modifiers) && !Modifier.isInterface(modifiers);
        Parameter[] parameters;
        int minimum = Integer.MAX_VALUE;
        for (Constructor<?> c : type.getDeclaredConstructors()) {
            constructors.add(c);
            if (concrete) {
                parameters = c.getParameters();
                // Identify the constructor with the fewest parameters
                if (parameters.length < minimum) {
                    minimumConstructor = c;
                    minimum = parameters.length;
                }
                switch (parameters.length) {
                    case 0:
                        // Default constructor
                        defaultConstructor = setAccessible(c);
                        break;
                    case 1:
                        // Single-argument constructor
                        defaultSingleConstructor = defaultSingleConstructor == null ? c : defaultSingleConstructor;
                        singleConstructors.put(ClassUtils.inbox(parameters[0].getType()), setAccessible(c));
                        break;
                }
            }
        }
        if (minimumConstructor != null) {
            minimumConstructor = (minimumConstructor == defaultConstructor || minimumConstructor == defaultSingleConstructor)
                    ? null : setAccessible(minimumConstructor);
        }
    }

    /**
     * Sets the given constructor to be accessible.
     *
     * @param constructor The constructor to make accessible.
     * @return The constructor, now accessible.
     */
    protected Constructor<?> setAccessible(final Constructor<?> constructor) {
        if (!constructor.isAccessible()) {
            constructor.setAccessible(true);
        }
        return constructor;
    }

    /**
     * Retrieves a single-argument constructor for the specified parameter type.
     *
     * @param type The parameter type of the constructor to retrieve.
     * @return The single-argument constructor, or null if not found.
     */
    public Constructor<?> getConstructor(final Class<?> type) {
        return type == null ? null : singleConstructors.get(type);
    }

    /**
     * Instantiates a new object of the managed class type.
     *
     * @param <T> The type of the object to be instantiated.
     * @return A new instance of the managed class type.
     */
    @SuppressWarnings("unchecked")
    public <T> T newInstance() {
        try {
            if (type.isMemberClass() && !Modifier.isStatic(type.getModifiers())) {
                if (defaultSingleConstructor != null) {
                    // Default constructor for inner class
                    return (T) defaultSingleConstructor.newInstance(new Object[]{null});
                }
            } else if (defaultConstructor != null) {
                // Default constructor
                return (T) defaultConstructor.newInstance();
            }
            if (minimumConstructor != null) {
                // Constructor with the fewest parameters, using default values
                Object[] parameters = new Object[minimumConstructor.getParameterCount()];
                int i = 0;
                for (Class<?> cl : minimumConstructor.getParameterTypes()) {
                    if (char.class == cl) {
                        parameters[i] = Character.MIN_VALUE;
                    } else if (boolean.class == cl) {
                        parameters[i] = false;
                    } else {
                        parameters[i] = cl.isPrimitive() ? 0 : null;
                    }
                }
                return (T) minimumConstructor.newInstance(parameters);
            }
            return null;
        } catch (Exception e) {
            throw new ReflectException("an error occurred while instance class. " + type.getName(), e);
        }
    }
}
