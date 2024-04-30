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
package com.jd.live.agent.core.inject.jbind.converter.supplier;

import com.jd.live.agent.core.inject.jbind.Conversion;
import com.jd.live.agent.core.inject.jbind.ConversionType;
import com.jd.live.agent.core.inject.jbind.Converter;
import com.jd.live.agent.core.inject.jbind.ConverterSupplier;
import com.jd.live.agent.core.util.type.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class FactoryMethodSupplier implements ConverterSupplier {

    protected final String methodName;

    protected FactoryMethodSupplier(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public Converter getConverter(ConversionType type) {
        Method method = getFactoryMethod(type.getTargetType().getRawType(), methodName, type.getSourceType().getRawType());
        return method == null ? null : new FactoryMethodConverter(method);
    }

    protected abstract ConcurrentMap<Class<?>, ConcurrentMap<Class<?>, Optional<Method>>> getCache();

    /**
     * Retrieves the factory method.
     *
     * @param cls           the target class
     * @param methodName    the name of the method
     * @param parameterType the type of the parameter
     * @return the method if found, null otherwise
     * @throws SecurityException if a security violation occurs
     */
    protected Method getFactoryMethod(final Class<?> cls, final String methodName, final Class<?> parameterType) throws SecurityException {
        if (cls == null || cls.isInterface() || parameterType == null) {
            return null;
        }
        ConcurrentMap<Class<?>, ConcurrentMap<Class<?>, Optional<Method>>> cache = getCache();
        ConcurrentMap<Class<?>, Optional<Method>> options = cache.computeIfAbsent(cls, t -> new ConcurrentHashMap<>());
        // The parameter has already been dealt with primitive types
        Optional<Method> option = options.get(parameterType);
        if (option == null) {
            // Fetching the constructor
            Method method = null;
            Method[] methods = cls.getDeclaredMethods();
            Class<?>[] parameters;
            // Iterating through constructors
            for (Method c : methods) {
                parameters = c.getParameterTypes();
                if (Modifier.isStatic(c.getModifiers())
                        && Modifier.isPublic(c.getModifiers())
                        && c.getName().equals(methodName)
                        && cls.isAssignableFrom(c.getReturnType())
                        && parameters.length == 1 && ClassUtils.inbox(parameters[0]).isAssignableFrom(parameterType)) {
                    // Single parameter, handling primitive types, if assignable directly
                    method = c;
                    break;
                }
            }

            option = Optional.ofNullable(method);
            Optional<Method> exist = options.putIfAbsent(parameterType, option);
            if (exist != null) {
                option = exist;
            }

        }
        return option.orElse(null);
    }


    public static class FactoryMethodConverter implements Converter {

        private final Method method;

        public FactoryMethodConverter(Method method) {
            if (!method.isAccessible())
                method.setAccessible(true);
            this.method = method;
        }

        @Override
        public Object convert(Conversion conversion) throws Exception {
            return conversion == null ? null : method.invoke(null, conversion.getSource());
        }
    }
}
