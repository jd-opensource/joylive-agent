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

import lombok.Getter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.jd.live.agent.core.util.type.TypeScanner.scanner;

/**
 * A utility class for storing and retrieving information about methods within a Java class.
 *
 * <p>This class offers a way to obtain all methods of a specified class, including getters and setters,
 * categorized by method names. It also provides a mechanism to retrieve getter and setter methods based on field names.</p>
 */
public class MethodList {

    private static final String GETTER_PREFIX = "get";

    private static final String IS_PREFIX = "is";

    private static final String SETTER_PREFIX = "set";

    protected Class<?> type;

    protected Map<String, Method> getter;

    protected Map<String, Method> setter;

    @Getter
    protected List<Method> methods;

    protected Map<String, List<Method>> methodNames;

    /**
     * Constructor that initializes an instance of MethodList.
     *
     * @param type      The {@link Class} object of the class to analyze.
     * @param predicate A predicate for determining if a field name meets certain conditions. If null, all field names are accepted.
     */
    public MethodList(Class<?> type, Predicate<String> predicate) {
        this.type = type;
        if (!type.isPrimitive() && !type.isArray()) {
            methods = new ArrayList<>();
            scanner(type).scan(cls -> Collections.addAll(methods, cls.getDeclaredMethods()));
            int size = methods.size();
            setter = new HashMap<>(size / 2);
            getter = new HashMap<>(size / 2);
            methodNames = new HashMap<>(size);
            String name;
            for (Method method : methods) {
                methodNames.computeIfAbsent(method.getName(), s -> new ArrayList<>()).add(method);
                PropertyMethod propertyMethod = PropertyMethod.getPropertyMethod(method);
                if (propertyMethod != null) {
                    name = propertyMethod.getName(method);
                    if ((predicate == null || predicate.test(name))) {
                        if (propertyMethod.isGetter()) {
                            getter.put(name, method);
                        } else {
                            setter.put(name, method);
                        }
                    }
                }
            }
        } else {
            setter = new HashMap<>(0);
            getter = new HashMap<>(0);
            methods = new ArrayList<>(0);
        }
    }

    public Method getSetter(final String name) {
        return setter.get(name);
    }

    public Method getGetter(final String name) {
        return getter.get(name);
    }

    public List<Method> getMethods(final String name) {
        return methodNames.get(name);
    }

    /**
     * Iterates over all the method and applies the given consumer to each one.
     *
     * @param consumer The consumer to be applied to each method.
     */
    public void forEach(final Consumer<Method> consumer) {
        if (consumer != null) {
            methods.forEach(consumer);
        }
    }

    /**
     * An enum representing the three types of property methods: getter, is, and setter.
     *
     * @author Your Name
     */
    protected enum PropertyMethod {

        /**
         * Represents a getter method.
         */
        GETTER {
            @Override
            public boolean match(Method method) {
                return !Modifier.isStatic(method.getModifiers())
                        && method.getName().startsWith(GETTER_PREFIX)
                        && method.getName().length() > 3
                        && method.getParameterCount() == 0
                        && void.class != method.getReturnType();
            }

            @Override
            public String getName(Method method) {
                String name = method.getName();
                return name.substring(3, 4).toLowerCase() + name.substring(4);
            }
        },
        /**
         * Represents an 'is' method.
         */
        IS_GETTER {
            @Override
            public boolean match(Method method) {
                return !Modifier.isStatic(method.getModifiers())
                        && method.getName().startsWith(IS_PREFIX)
                        && method.getName().length() > 2
                        && method.getParameterCount() == 0
                        && boolean.class == method.getReturnType();
            }

            @Override
            public String getName(Method method) {
                String name = method.getName();
                return name.substring(2, 3).toLowerCase() + name.substring(3);
            }
        },
        /**
         * Represents a setter method.
         */
        SETTER {
            @Override
            public boolean isGetter() {
                return false;
            }

            @Override
            public boolean match(Method method) {
                return !Modifier.isStatic(method.getModifiers())
                        && method.getName().startsWith(SETTER_PREFIX)
                        && method.getName().length() > 3
                        && method.getParameterCount() == 1;
            }

            @Override
            public String getName(Method method) {
                String name = method.getName();
                return name.substring(3, 4).toLowerCase() + name.substring(4);
            }
        };

        /**
         * Checks if the given method is a getter method.
         *
         * @return true if the method is a getter, false otherwise
         */
        public boolean isGetter() {
            return true;
        }

        /**
         * Checks if the given method matches the criteria for this property method type.
         *
         * @param method the method to check
         * @return true if the method matches, false otherwise
         */
        public abstract boolean match(Method method);

        /**
         * Extracts the property name from the given method.
         *
         * @param method the method to extract the property name from
         * @return the property name
         */
        public abstract String getName(Method method);

        /**
         * Gets the PropertyMethod instance that corresponds to the given method.
         *
         * @param method the method to find the PropertyMethod for
         * @return the PropertyMethod instance, or null if no match is found
         */
        public static PropertyMethod getPropertyMethod(Method method) {
            for (PropertyMethod propertyMethod : PropertyMethod.values()) {
                if (propertyMethod.match(method)) {
                    return propertyMethod;
                }
            }
            return null;
        }

    }

}
