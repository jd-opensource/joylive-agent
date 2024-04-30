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

import com.jd.live.agent.core.bytekit.type.AnnotationDesc;
import com.jd.live.agent.core.bytekit.type.MethodDesc;
import com.jd.live.agent.core.bytekit.type.ParameterDesc;
import com.jd.live.agent.core.bytekit.type.TypeDesc;
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

    public BuddyParameterDesc(ParameterDescription desc) {
        this.desc = desc;
    }

    @Override
    public List<AnnotationDesc> getDeclaredAnnotations() {
        return desc.getDeclaredAnnotations().stream().map(BuddyAnnotationDesc::new).collect(Collectors.toList());
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
        return new BuddyMethodDesc(desc.getDeclaringMethod());
    }

    @Override
    public TypeDesc.Generic getType() {
        return new BuddyTypeDesc.BuddyGeneric(desc.getType());
    }
}
