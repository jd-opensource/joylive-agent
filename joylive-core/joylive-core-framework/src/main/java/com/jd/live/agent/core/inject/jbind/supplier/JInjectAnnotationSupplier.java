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
package com.jd.live.agent.core.inject.jbind.supplier;

import com.jd.live.agent.bootstrap.classloader.ResourcerType;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.ExtensibleDesc;
import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.Injection;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.InjectLoader;
import com.jd.live.agent.core.inject.jbind.*;
import com.jd.live.agent.core.inject.jbind.InjectType.InjectField;
import com.jd.live.agent.core.util.cache.CacheObject;
import com.jd.live.agent.core.util.type.FieldDesc;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * scan field with @inject annotation and then build an injection.
 *
 * @since 1.0.0
 */
@Extension(value = InjectionSupplier.INJECT_ANNOTATION_SUPPLIER)
public class JInjectAnnotationSupplier extends AbstractAnnotationSupplier {

    private final Map<Class<?>, CacheObject<Injection>> types = new ConcurrentHashMap<>(1000);

    @Override
    protected Map<Class<?>, CacheObject<Injection>> getCache(Class<?> type, InjectionContext context) {
        return types;
    }

    @Override
    protected InjectField createField(InjectType injectType, FieldDesc fieldDesc, InjectionContext context) {
        Inject inject = fieldDesc.getAnnotation(Inject.class);
        if (inject != null) {
            String key = inject.value();
            Sourcer sourcer = createdSourcer(fieldDesc, key);
            return new InjectField(fieldDesc.getField(), key, fieldDesc, inject, fieldDesc.getGeneric(), sourcer);
        }
        return null;
    }

    protected Sourcer createdSourcer(FieldDesc fieldDesc, String name) {
        Class<?> fieldType = fieldDesc.getType();
        if (fieldType.equals(Publisher.class)) {
            return new JPublisherSourcer(name, fieldType);
        } else if (fieldType.equals(ExtensibleDesc.class)) {
            return build(getExtensibleByList(fieldDesc), name, fieldDesc, JExtensibleSourcer::new);
        } else if (fieldType.equals(List.class)) {
            return build(getExtensibleByList(fieldDesc), name, fieldDesc, JExtensionListSourcer::new);
        } else if (fieldType.isArray()) {
            return build(getExtensibleByArray(fieldDesc), name, fieldDesc, JExtensionArraySourcer::new);
        } else if (fieldType.equals(Map.class)) {
            return build(getExtensibleByMap(fieldDesc), name, fieldDesc, JExtensionMapSourcer::new);
        } else if (fieldType.isInterface()) {
            return build(fieldType, name, fieldDesc, JExtensionSourcer::new);
        }
        return new JComponentSourcer(name, fieldType);
    }

    protected Sourcer build(Class<?> extensible, String name, FieldDesc fieldDesc, SourcerFactory factory) {
        if (extensible != null && extensible.isInterface() && extensible.isAnnotationPresent(Extensible.class)) {
            InjectLoader injectLoader = fieldDesc.getAnnotation(InjectLoader.class);
            return factory.build(name, extensible, fieldDesc.getOwner(), injectLoader == null ? null : injectLoader.value());
        }
        return new JComponentSourcer(name, fieldDesc.getType());
    }

    @Override
    protected Injection createInjection(InjectType injectType, InjectionContext context) {
        return new JInjectAnnotationInjection(injectType);
    }

    protected Class<?> getExtensibleByArray(FieldDesc fieldDesc) {
        Type type = getComponentType(fieldDesc);
        return type instanceof Class ? (Class<?>) type : null;
    }

    protected Class<?> getExtensibleByList(FieldDesc fieldDesc) {
        Type[] types = getParameterizedTypes(fieldDesc);
        return types != null && types.length > 0 && types[0] instanceof Class ? (Class<?>) types[0] : null;
    }

    protected Class<?> getExtensibleByMap(FieldDesc fieldDesc) {
        Type[] types = getParameterizedTypes(fieldDesc);
        if (types != null && types.length == 2 && types[0].equals(String.class)) {
            if (types[1] instanceof Class<?>) {
                return (Class<?>) types[1];
            } else if (types[1] instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) types[1];
                return (Class<?>) parameterizedType.getRawType();
            }
        }
        return null;
    }

    protected Type[] getParameterizedTypes(FieldDesc fieldDesc) {
        Type genericType = fieldDesc.getGeneric().getType();
        if (genericType instanceof ParameterizedType) {
            return ((ParameterizedType) genericType).getActualTypeArguments();
        }
        return null;
    }

    protected Type getComponentType(FieldDesc fieldDesc) {
        Type genericType = fieldDesc.getGeneric().getType();
        if (genericType instanceof Class) {
            return ((Class<?>) genericType).getComponentType();
        } else if (genericType instanceof GenericArrayType) {
            return ((GenericArrayType) genericType).getGenericComponentType();
        }
        return null;
    }

    @FunctionalInterface
    protected interface SourcerFactory {

        Sourcer build(String name, Class<?> type, Class<?> owner, ResourcerType resourcerType);

    }
}
