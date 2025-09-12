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

import com.jd.live.agent.core.bytekit.type.*;
import net.bytebuddy.description.method.ParameterDescription;

import java.util.List;
import java.util.stream.Collectors;

/**
 * BuddyParameterDesc
 *
 * @since 1.0.0
 */
public class BuddyParameterDesc implements ParameterDesc {

    private final ParameterDescription desc;
    private final TypePool typePool;

    /**
     * Updated constructor to accept the TypePool context.
     *
     * @param desc     The Byte Buddy parameter description to wrap.
     * @param typePool The TypePool context.
     */
    public BuddyParameterDesc(ParameterDescription desc, TypePool typePool) {
        this.desc = desc;
        this.typePool = typePool;
    }

    @Override
    public List<AnnotationDesc> getDeclaredAnnotations() {
        return desc.getDeclaredAnnotations().stream()
                .map(annotationDesc -> new BuddyAnnotationDesc(annotationDesc, this.typePool))
                .collect(Collectors.toList());
    }

    @Override
    public String getActualName() {
        return desc.getActualName();
    }

    @Override
    public int getIndex() {
        return desc.getIndex();
    }

    @Override
    public MethodDesc getDeclaringMethod() {
        return new BuddyMethodDesc(desc.getDeclaringMethod(), this.typePool);
    }

    @Override
    public TypeDesc.Generic getType() {
        return new BuddyTypeDesc.BuddyGeneric(desc.getType(), this.typePool);
    }
}
