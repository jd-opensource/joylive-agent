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
import com.jd.live.agent.core.bytekit.type.TypeDesc;
import com.jd.live.agent.core.bytekit.type.TypePool;
import net.bytebuddy.description.annotation.AnnotationDescription;

/**
 * BuddyAnnotationDesc
 *
 * @since 1.0.0
 */
public class BuddyAnnotationDesc implements AnnotationDesc {

    private final AnnotationDescription desc;
    private final TypePool typePool; // <<< 1. 添加 TypePool 字段

    /**
     * Updated constructor to accept the TypePool context.
     *
     * @param desc     The Byte Buddy annotation description to wrap.
     * @param typePool The TypePool context needed for creating further type descriptions.
     */
    public BuddyAnnotationDesc(AnnotationDescription desc, TypePool typePool) {
        this.desc = desc;
        this.typePool = typePool;
    }

    @Override
    public TypeDesc getAnnotationType() {
        return new BuddyTypeDesc(desc.getAnnotationType(), this.typePool);
    }
}
