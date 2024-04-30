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
package com.jd.live.agent.core.inject.jbind.converter;

import com.jd.live.agent.core.inject.jbind.*;
import com.jd.live.agent.core.inject.jbind.converter.array.StringArray;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static com.jd.live.agent.core.util.StringUtils.SEMICOLON_COMMA;
import static com.jd.live.agent.core.util.StringUtils.split;
import static com.jd.live.agent.core.util.type.ClassUtils.inbox;

/**
 * Provides a base implementation for converting objects between different polymorphic types.
 * This abstract class defines the framework for conversion, including methods for creating source and target
 * collection-like structures, determining component types, and performing the conversion process.
 */
public abstract class Abstract2PolyConverter implements Converter {

    /**
     * Creates a source collection-like structure from the given conversion context and component type.
     * This method should be implemented to construct an appropriate source structure for the conversion.
     *
     * @param conversion    The conversion context containing information about the conversion process.
     * @param componentType The component type of the source structure.
     * @return A {@link PolyObject} representing the source structure.
     */
    protected abstract PolyObject createSourceSet(Conversion conversion, Class<?> componentType);

    /**
     * Creates a target collection-like structure from the given conversion context, component type, and size.
     * This method should be implemented to construct an appropriate target structure for the conversion.
     *
     * @param conversion    The conversion context containing information about the conversion process.
     * @param componentType The component type of the target structure.
     * @param size          The initial size of the target structure.
     * @return A {@link PolyObject} representing the target structure.
     * @throws Exception If there is an error during the creation of the target structure.
     */
    protected abstract PolyObject createTargetSet(Conversion conversion, Class<?> componentType, int size) throws Exception;

    /**
     * Determines the component type of the source based on the given conversion context.
     *
     * @param conversion The conversion context containing information about the conversion process.
     * @return The component type of the source.
     */
    protected abstract Class<?> getSourceComponentType(Conversion conversion);

    /**
     * Attempts to create a collection of the specified target type and size.
     * This method supports creation of {@link List}, {@link Set}, and {@link SortedSet} types.
     *
     * @param targetType The class of the target collection.
     * @param size       The initial size of the collection.
     * @return A new collection instance of the specified type and size, or {@code null} if the type is not supported.
     * @throws Exception If there is an error during the creation of the collection.
     */
    @SuppressWarnings("unchecked")
    protected Collection<Object> createCollection(final Class<?> targetType, final int size) throws Exception {
        if (targetType == null) {
            return null;
        } else if (targetType.equals(List.class)) {
            return new ArrayList<>(size);
        } else if (targetType.equals(Set.class)) {
            return new HashSet<>(size);
        } else if (targetType.equals(SortedSet.class)) {
            return new TreeSet<>();
        } else if (targetType.isInterface()) {
            return null;
        } else if (Modifier.isAbstract(targetType.getModifiers())) {
            return null;
        } else {
            return (Collection<Object>) targetType.newInstance();
        }
    }

    /**
     * Determines the component type of the target based on the given conversion context.
     *
     * @param conversion The conversion context containing information about the conversion process.
     * @return The component type of the target.
     */
    protected Class<?> getTargetComponentType(Conversion conversion) {
        Type type = conversion.getTargetType().getType();
        if (type instanceof ParameterizedType) {
            type = ((ParameterizedType) type).getActualTypeArguments()[0];
            return (Class<?>) type;
        } else if (type instanceof GenericArrayType) {
            type = ((GenericArrayType) type).getGenericComponentType();
            return (Class<?>) type;
        } else {
            return conversion.getTargetType().getRawType().getComponentType();
        }
    }

    /**
     * Checks if the target component type is assignable from the source component type.
     *
     * @param targetComponentType The target component type.
     * @param sourceComponentType The source component type.
     * @return {@code true} if assignment is possible; {@code false} otherwise.
     */
    protected boolean isAssignable(Class<?> targetComponentType, Class<?> sourceComponentType) {
        return false;
    }

    /**
     * Checks if the given source element is considered empty.
     *
     * @param sourceElement The source element to check.
     * @return {@code true} if the source element is considered empty; {@code false} otherwise.
     */
    protected boolean isEmpty(Object sourceElement) {
        return sourceElement == null;
    }

    @Override
    public Object convert(Conversion conversion) throws Exception {
        Class<?> targetComponentCls = getTargetComponentType(conversion);
        Class<?> sourceComponentCls = getSourceComponentType(conversion);
        Class<?> inboxTargetComponentCls = inbox(targetComponentCls);
        Class<?> inboxSourceComponentCls = inbox(sourceComponentCls);
        if (inboxTargetComponentCls == null) {
            return null;
        }
        if (isAssignable(targetComponentCls, sourceComponentCls)) return conversion.getSource();
        TypeInfo sourceComponentType = new TypeInfo(sourceComponentCls, inboxSourceComponentCls);
        TypeInfo targetComponentType = new TypeInfo(targetComponentCls, inboxTargetComponentCls, inboxTargetComponentCls);
        Converter defaultConverter = inboxSourceComponentCls == null ? null : conversion.getConverter(new ConversionType(sourceComponentType, targetComponentType));
        PolyObject sourceSet = createSourceSet(conversion, sourceComponentCls);
        PolyObject targetSet = createTargetSet(conversion, targetComponentCls, sourceSet.size());
        if (targetSet == null) {
            return null;
        }
        Object sourceElement, targetElement;
        Iterator<Object> sourceIt = sourceSet.iterator();
        Converter sourceElementConverter = null;
        TypeInfo sourceElementType;
        Class<?> sourceElementCls;
        Class<?> inboxSourceElementCls, inboxLastSourceElementCls = null;
        String path = conversion.getPath();
        int index = -1;
        while (sourceIt.hasNext()) {
            index++;
            sourceElement = sourceIt.next();
            if (isEmpty(sourceElement)) {
                if (targetComponentCls.isPrimitive()) {
                    return null;
                } else {
                    targetSet.add(null);
                }
            } else {
                if (defaultConverter == null) {
                    sourceElementCls = sourceElement.getClass();
                    inboxSourceElementCls = inbox(sourceElementCls);
                    sourceElementType = new TypeInfo(sourceElementCls, inboxSourceElementCls);
                    sourceElementConverter = inboxSourceElementCls.equals(inboxLastSourceElementCls) ? sourceElementConverter :
                            conversion.getConverter(new ConversionType(sourceElementType, targetComponentType));
                    if (sourceElementConverter == null) {
                        return null;
                    }
                    inboxLastSourceElementCls = inboxSourceElementCls;
                } else {
                    sourceElementConverter = defaultConverter;
                    sourceElementType = sourceComponentType;
                }
                Conversion conv = conversion.of(sourceElementType, targetComponentType, sourceElement);
                conv.setPath(path + "[" + index + "]");
                targetElement = sourceElementConverter.convert(conv);
                conv.setPath(path);
                if (targetElement == null) {
                    return null;
                }
                targetSet.add(sourceElement);
            }
        }
        return targetSet.getTarget();
    }

    /**
     * An abstract converter for converting arrays to polymorphic types.
     */
    public static abstract class Array2PolyConverter extends Abstract2PolyConverter {
        @Override
        protected PolyObject createSourceSet(Conversion conversion, Class<?> componentType) {
            return new PolyObject.ArrayPolyObject(conversion.getArrayBuilder(componentType).create(conversion.getSource()));
        }

        @Override
        protected Class<?> getSourceComponentType(Conversion conversion) {
            return conversion.getSourceType().getRawType().getComponentType();
        }

    }

    /**
     * An abstract converter for converting collections to polymorphic types.
     */
    public static abstract class Collection2PolyConverter extends Abstract2PolyConverter {

        @SuppressWarnings("unchecked")
        @Override
        protected PolyObject createSourceSet(Conversion conversion, Class<?> componentType) {
            return new PolyObject.CollectionPolyObject((Collection<Object>) conversion.getSource());
        }

        @Override
        protected Class<?> getSourceComponentType(Conversion conversion) {
            return null;
        }
    }

    /**
     * An abstract converter for converting strings to polymorphic types.
     */
    public static abstract class String2PolyConverter extends Abstract2PolyConverter {

        @Override
        protected PolyObject createSourceSet(Conversion conversion, Class<?> componentType) {
            String[] parts = split(conversion.getSource().toString(), SEMICOLON_COMMA, String::trim);
            return new PolyObject.ArrayPolyObject(new StringArray(parts));
        }

        @Override
        protected Class<?> getSourceComponentType(Conversion conversion) {
            return String.class;
        }

        @Override
        protected boolean isEmpty(Object sourceElement) {
            return sourceElement == null || ((String) sourceElement).isEmpty();
        }
    }

}
