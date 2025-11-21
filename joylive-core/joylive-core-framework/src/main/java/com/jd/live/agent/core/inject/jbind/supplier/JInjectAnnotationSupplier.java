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
            Sourcer sourcer = createdSourcer(fieldDesc, inject, key);
            return new InjectField(fieldDesc.getField(), key, fieldDesc, inject, fieldDesc.getGeneric(), sourcer);
        }
        return null;
    }

    /**
     * Creates a Sourcer based on the given FieldDesc and name.
     *
     * @param fieldDesc the FieldDesc object containing information about the field
     * @param inject    the inject annotation
     * @param name      the name of the Sourcer
     * @return the created Sourcer object
     */
    protected Sourcer createdSourcer(FieldDesc fieldDesc, Inject inject, String name) {
        Class<?> fieldType = fieldDesc.getType();
        if (!inject.component()) {
            if (fieldType.equals(Publisher.class)) {
                return new JPublisherSourcer(name, fieldType);
            } else if (fieldType.equals(ExtensibleDesc.class)) {
                return build(fieldDesc, inject, name, getExtensibleByList(fieldDesc), JExtensibleSourcer::new);
            } else if (fieldType.equals(List.class)) {
                return build(fieldDesc, inject, name, getExtensibleByList(fieldDesc), JExtensionListSourcer::new);
            } else if (fieldType.isArray()) {
                return build(fieldDesc, inject, name, getExtensibleByArray(fieldDesc), JExtensionArraySourcer::new);
            } else if (fieldType.equals(Map.class)) {
                return build(fieldDesc, inject, name, getExtensibleByMap(fieldDesc), JExtensionMapSourcer::new);
            } else if (fieldType.isInterface()) {
                return build(fieldDesc, inject, name, fieldType, JExtensionSourcer::new);
            }
        }
        return new JComponentSourcer(name, fieldType);
    }

    /**
     * Builds a Sourcer based on the given extensible class, name, field description, and factory.
     *
     * @param fieldDesc the field description
     * @param inject    the inject annotation
     * @param name      the name of the Sourcer
     * @param extensible the extensible class type
     * @param factory the factory to create the Sourcer
     * @return a new Sourcer instance
     */
    protected Sourcer build(FieldDesc fieldDesc, Inject inject, String name, Class<?> extensible, SourcerFactory factory) {
        if (extensible != null && extensible.isInterface() && extensible.isAnnotationPresent(Extensible.class)) {
            switch (inject.loader()) {
                case CORE_IMPL:
                    return factory.build(name, extensible, fieldDesc.getOwner(), ResourcerType.CORE_IMPL);
                case CORE:
                    return factory.build(name, extensible, fieldDesc.getOwner(), ResourcerType.CORE);
                case PLUGIN:
                    return factory.build(name, extensible, fieldDesc.getOwner(), ResourcerType.PLUGIN);
                default:
                    return factory.build(name, extensible, fieldDesc.getOwner(), ResourcerType.CORE_IMPL);
            }

        }
        return new JComponentSourcer(name, fieldDesc.getType());
    }

    @Override
    protected Injection createInjection(InjectType injectType, InjectionContext context) {
        return new JInjectAnnotationInjection(injectType);
    }

    /**
     * Retrieves the extensible class type from an array field.
     *
     * @param fieldDesc the field description
     * @return the component class type if the field is an array, or null otherwise
     */
    protected Class<?> getExtensibleByArray(FieldDesc fieldDesc) {
        Type type = getComponentType(fieldDesc);
        return type instanceof Class ? (Class<?>) type : null;
    }

    /**
     * Retrieves the extensible class type from a list field.
     *
     * @param fieldDesc the field description
     * @return the parameterized class type of the list if available, or null otherwise
     */
    protected Class<?> getExtensibleByList(FieldDesc fieldDesc) {
        Type[] types = getParameterizedTypes(fieldDesc);
        if (types == null || types.length == 0) {
            return null;
        } else if (types[0] instanceof Class<?>) {
            return (Class<?>) types[0];
        } else if (types[0] instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) types[0];
            if (pType.getRawType() instanceof Class<?>) {
                return (Class<?>) pType.getRawType();
            }
        }
        return null;
    }

    /**
     * Retrieves the extensible class type from a map field.
     *
     * @param fieldDesc the field description
     * @return the class type of the map's value if the key is a String and the value type is a class or parameterized type, or null otherwise
     */
    protected Class<?> getExtensibleByMap(FieldDesc fieldDesc) {
        Type[] types = getParameterizedTypes(fieldDesc);
        if (types != null && types.length == 2 && types[0].equals(String.class)) {
            if (types[1] instanceof Class<?>) {
                return (Class<?>) types[1];
            } else if (types[1] instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) types[1];
                if (pType.getRawType() instanceof Class<?>) {
                    return (Class<?>) pType.getRawType();
                }
            }
        }
        return null;
    }

    /**
     * Get the parameterized types of a field.
     *
     * @param fieldDesc the field description
     * @return an array of Type objects representing the actual type arguments of the parameterized type, or null if the type is not parameterized
     */
    protected Type[] getParameterizedTypes(FieldDesc fieldDesc) {
        Type genericType = fieldDesc.getGeneric().getType();
        if (genericType instanceof ParameterizedType) {
            return ((ParameterizedType) genericType).getActualTypeArguments();
        }
        return null;
    }

    /**
     * Get the component type of a field.
     *
     * @param fieldDesc the field description
     * @return the component type of the array if the field type is an array, or null if the field type is not an array
     */
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
