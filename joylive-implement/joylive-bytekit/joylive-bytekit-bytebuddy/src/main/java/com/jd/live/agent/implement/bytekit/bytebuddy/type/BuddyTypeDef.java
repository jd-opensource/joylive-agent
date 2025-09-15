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
import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.implement.bytekit.bytebuddy.type.BuddyTypeDesc.BuddyGeneric;
import com.jd.live.agent.implement.bytekit.bytebuddy.util.PoolUtil;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.pool.TypePool;

import java.util.ArrayList;
import java.util.List;

/**
 * BuddyTypeDef
 *
 * @since 1.0.0
 */
public abstract class BuddyTypeDef<T extends TypeDefinition> implements TypeDef {

    protected final T desc;

    protected final ClassLoader classLoader;

    protected final LazyObject<T> safeDesc;

    public BuddyTypeDef(T desc, ClassLoader classLoader) {
        this.desc = desc;
        this.classLoader = classLoader;
        this.safeDesc = new LazyObject<>(() -> {
            if (desc instanceof TypeDescription.ForLoadedType) {
                // avoid class loading conflict with other agent.
                TypeDescription resolved = PoolUtil.getTypePool(classLoader).describe(desc.getActualName()).resolve();
                return convert(resolved);
            }
            return desc;
        });
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
        MethodList<?> methods = desc.getDeclaredMethods();
        List<MethodDesc> result = new ArrayList<>(methods.size());
        methods.forEach(method -> result.add(new BuddyMethodDesc(method, classLoader)));
        return result;
    }

    @Override
    public TypeDesc asErasure() {
        return new BuddyTypeDesc(desc.asErasure(), classLoader);
    }

    @Override
    public TypeDesc.Generic getSuperClass() {
        try {
            T def = safeDesc.get();
            TypeDescription.Generic superClass = def.getSuperClass();
            return superClass == null ? null : new BuddyGeneric(superClass, classLoader);
        } catch (TypePool.Resolution.NoSuchTypeException e) {
            return null;
        }
    }

    @Override
    public List<TypeDesc.Generic> getInterfaces() {
        try {
            T def = safeDesc.get();
            TypeList.Generic interfaces = def.getInterfaces();
            List<TypeDesc.Generic> result = new ArrayList<>(interfaces.size());
            for (TypeDescription.Generic type : interfaces) {
                result.add(new BuddyGeneric(type, classLoader));
            }
            return result;
        } catch (TypePool.Resolution.NoSuchTypeException ignored) {
            return new ArrayList<>();
        }
    }

    protected abstract T convert(TypeDescription resolved);
}
