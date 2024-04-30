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
import net.bytebuddy.description.type.TypeDescription;

import java.util.List;
import java.util.stream.Collectors;

/**
 * BuddyTypeDesc
 *
 * @since 1.0.0
 */
public class BuddyTypeDesc extends BuddyTypeDef<TypeDescription> implements TypeDesc {

    public BuddyTypeDesc(TypeDescription desc) {
        super(desc);
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
    public TypeDesc getComponentType() {
        return new BuddyTypeDesc(desc.getComponentType());
    }

    @Override
    public List<AnnotationDesc> getDeclaredAnnotations() {
        return desc.getDeclaredAnnotations().stream().map(BuddyAnnotationDesc::new).collect(Collectors.toList());
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
    public String getSimpleName() {
        return desc.getSimpleName();
    }

    public static class BuddyGeneric extends BuddyTypeDef<TypeDescription.Generic> implements Generic {

        public BuddyGeneric(TypeDescription.Generic generic) {
            super(generic);
        }

        @Override
        public Generic getComponentType() {
            return new BuddyGeneric(desc.getComponentType());
        }
    }
}
