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
package com.jd.live.agent.implement.bytekit.bytebuddy.type;

import com.jd.live.agent.core.bytekit.type.TypePool;
import com.jd.live.agent.core.bytekit.type.AnnotationDesc;
import com.jd.live.agent.core.bytekit.type.TypeDesc;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;

import java.util.List;
import java.util.stream.Collectors;

/**
 * BuddyTypeDesc
 *
 * @since 1.0.0
 */
public class BuddyTypeDesc extends BuddyTypeDef<TypeDescription> implements TypeDesc {

    /**
     * The pool that created this type description. It's needed for further type lookups.
     */
    private final TypePool typePool;

    // TODO typePool may not be null
    public BuddyTypeDesc(TypeDescription desc) {
        super(desc);
        typePool = null;
    }

    /**
     * The primary constructor that includes the TypePool.
     *
     * @param desc     The Byte Buddy type description to wrap.
     * @param typePool The pool this description belongs to.
     */
    public BuddyTypeDesc(TypeDescription desc, TypePool typePool) {
        super(desc);
        if (typePool == null) {
            throw new IllegalArgumentException("TypePool cannot be null for " + desc.getName());
        }
        this.typePool = typePool;
    }

    @Override
    public int getModifiers() {
        return desc.getModifiers();
    }

    @Override
    public boolean isFinal() {
        return desc.isFinal();
    }

    @Override
    public boolean isStatic() {
        return desc.isStatic();
    }

    @Override
    public boolean isPublic() {
        return desc.isPublic();
    }

    @Override
    public boolean isProtected() {
        return desc.isProtected();
    }

    @Override
    public boolean isPrivate() {
        return desc.isPrivate();
    }

    @Override
    public boolean isAssignableFrom(Class<?> type) {
        return desc.isAssignableFrom(type);
    }

    @Override
    public boolean isAssignableTo(Class<?> type) {
        return desc.isAssignableTo(type);
    }

    @Override
    public TypeDesc getComponentType() {
        TypeDescription componentType = desc.getComponentType();
        // Propagate the TypePool to the component type description as well.
        return componentType == null ? null : new BuddyTypeDesc(componentType, this.typePool);
    }

    @Override
    public List<AnnotationDesc> getDeclaredAnnotations() {
        return desc.getDeclaredAnnotations().stream().map(BuddyAnnotationDesc::new).collect(Collectors.toList());
    }

    @Override
    public String getSuperName() {
        TypeDescription.Generic superClass = desc.getSuperClass();
        // For java.lang.Object or interfaces, superClass is null.
        if (superClass == null) {
            return null;
        }
        // This is a safe operation that reads the name from bytecode metadata.
        return superClass.asErasure().getName();
    }

    @Override
    public String[] getInterfaceNames() {
        TypeList.Generic interfaces = desc.getInterfaces();
        return interfaces.stream()
                .map(iface -> iface.asErasure().getName())
                .toArray(String[]::new);
    }

    @Override
    public TypePool getTypePool() {
        return typePool;
    }

    public static class BuddyGeneric extends BuddyTypeDef<TypeDescription.Generic> implements Generic {

        public BuddyGeneric(TypeDescription.Generic generic) {
            super(generic);
        }

        @Override
        public Generic getComponentType() {
            TypeDescription.Generic componentType = desc.getComponentType();
            return componentType == null ? null : new BuddyGeneric(componentType);
        }
    }
}
