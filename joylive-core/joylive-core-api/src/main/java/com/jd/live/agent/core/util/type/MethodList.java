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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

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
            Method[] allMethods = type.getDeclaredMethods();
            setter = new HashMap<>(allMethods.length / 2);
            getter = new HashMap<>(allMethods.length / 2);
            methods = new ArrayList<>(allMethods.length);
            methodNames = new HashMap<>(allMethods.length);
            String name;
            for (Method method : allMethods) {
                if (!method.getDeclaringClass().equals(Object.class)) {
                    methods.add(method);
                    List<Method> methodList = methodNames.computeIfAbsent(method.getName(), s -> new ArrayList<>());
                    methodList.add(method);
                    if (!Modifier.isStatic(method.getModifiers())) {
                        name = method.getName();
                        if (name.startsWith(GETTER_PREFIX)) {
                            if (name.length() > 3 && method.getParameterCount() == 0
                                    && void.class != method.getReturnType()) {
                                name = name.substring(3, 4).toLowerCase() + name.substring(4);
                                if ((predicate == null || predicate.test(name))) {
                                    getter.put(name, method);
                                }
                            }
                        } else if (name.startsWith(IS_PREFIX)) {
                            if (name.length() > 2 && method.getParameterCount() == 0
                                    && boolean.class == method.getReturnType()) {
                                name = name.substring(2, 3).toLowerCase() + name.substring(3);
                                if ((predicate == null || predicate.test(name))) {
                                    getter.put(name, method);
                                }
                            }
                        } else if (name.startsWith(SETTER_PREFIX)) {
                            if (name.length() > 3 && method.getParameterCount() == 1) {
                                name = name.substring(3, 4).toLowerCase() + name.substring(4);
                                if ((predicate == null || predicate.test(name))) {
                                    setter.put(name, method);
                                }
                            }
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
}
