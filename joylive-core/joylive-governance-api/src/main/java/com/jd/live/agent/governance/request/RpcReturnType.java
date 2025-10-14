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
package com.jd.live.agent.governance.request;

import lombok.Getter;

import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * Represents a generic type with a specified type and return type.
 */
@Getter
public class RpcReturnType {

    private final ClassLoader classLoader;

    private final Class<?> returnClass;

    private final Type returnType;

    private final String generic;

    private final Function<Object, Object> converter;

    public RpcReturnType(ClassLoader classLoader, Class<?> returnClass, Type returnType) {
        this(classLoader, returnClass, returnType, null, null);
    }

    public RpcReturnType(ClassLoader classLoader, Class<?> returnClass, Type returnType, String generic) {
        this(classLoader, returnClass, returnType, generic, null);
    }

    public RpcReturnType(ClassLoader classLoader, Class<?> returnClass, Type returnType, String generic, Function<Object, Object> converter) {
        this.generic = generic;
        this.returnClass = returnClass;
        this.returnType = returnType;
        this.converter = converter;
        this.classLoader = classLoader;
    }

    public boolean isGeneric() {
        return generic != null;
    }

    public Object convert(Object value) {
        return converter == null ? value : converter.apply(value);
    }

}
