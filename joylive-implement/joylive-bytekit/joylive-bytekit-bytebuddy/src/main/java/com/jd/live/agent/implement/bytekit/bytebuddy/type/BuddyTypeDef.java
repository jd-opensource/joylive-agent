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

import com.jd.live.agent.core.bytekit.type.MethodDesc;
import com.jd.live.agent.core.bytekit.type.TypeDef;
import com.jd.live.agent.core.bytekit.type.TypeDesc;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.pool.TypePool;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BuddyTypeDef
 *
 * @since 1.0.0
 */
public abstract class BuddyTypeDef<T extends TypeDefinition> implements TypeDef {

    protected final T desc;

    public BuddyTypeDef(T desc) {
        this.desc = desc;
    }

    @Override
    public String getActualName() {
        return desc.getActualName();
    }

    @Override
    public boolean isInterface() {
        return desc.isInterface();
    }

    @Override
    public boolean isEnum() {
        return desc.isEnum();
    }

    @Override
    public boolean isArray() {
        return desc.isArray();
    }

    @Override
    public boolean isPrimitive() {
        return desc.isPrimitive();
    }

    @Override
    public boolean isAnnotation() {
        return desc.isAnnotation();
    }

    @Override
    public List<MethodDesc> getDeclaredMethods() {
        return desc.getDeclaredMethods().stream().map(BuddyMethodDesc::new).collect(Collectors.toList());
    }

    @Override
    public TypeDesc asErasure() {
        return new BuddyTypeDesc(desc.asErasure());
    }

    @Override
    public TypeDesc.Generic getSuperClass() {
        try {
            TypeDescription.Generic superClass = desc.getSuperClass();
            return superClass == null ? null : new BuddyTypeDesc.BuddyGeneric(superClass);
        } catch (TypePool.Resolution.NoSuchTypeException e) {
            return null;
        }
    }

    @Override
    public List<TypeDesc.Generic> getInterfaces() {
        try {
            return desc.getInterfaces().stream().map(BuddyTypeDesc.BuddyGeneric::new).collect(Collectors.toList());
        } catch (TypePool.Resolution.NoSuchTypeException e) {
            return new ArrayList<>();
        }
    }
}
