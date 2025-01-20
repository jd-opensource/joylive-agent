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
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;

import java.util.List;
import java.util.stream.Collectors;

/**
 * BuddyMethodDesc
 *
 * @since 1.0.0
 */
public class BuddyMethodDesc implements MethodDesc {

    private final MethodDescription desc;

    public BuddyMethodDesc(MethodDescription desc) {
        this.desc = desc;
    }

    @Override
    public List<AnnotationDesc> getDeclaredAnnotations() {
        return desc.getDeclaredAnnotations().stream().map(BuddyAnnotationDesc::new).collect(Collectors.toList());
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
    public boolean isConstructor() {
        return desc.isConstructor();
    }

    @Override
    public boolean isMethod() {
        return desc.isMethod();
    }

    @Override
    public boolean isDefaultMethod() {
        return desc.isDefaultMethod();
    }

    @Override
    public String getActualName() {
        return desc.getActualName();
    }

    @Override
    public List<ParameterDesc> getParameters() {
        return desc.getParameters().stream().map(BuddyParameterDesc::new).collect(Collectors.toList());
    }

    @Override
    public String getDescription() {
        return getDescription(desc);
    }

    /**
     * Generates a description of a method based on its declaring type, name, and parameter types.
     *
     * @param desc the method description to generate a description for
     * @return a string representing the method description
     */
    public static String getDescription(MethodDescription desc) {
        StringBuilder sb = new StringBuilder(256).append(desc.getDeclaringType().asErasure().getTypeName());
        if (desc.isConstructor()) {
            sb.append("#<init>(");
        } else {
            sb.append('#').append(desc.getActualName()).append("(");
        }
        int i = 0;
        for (ParameterDescription parameter : desc.getParameters()) {
            if (i++ > 0) {
                sb.append(',');
            }
            sb.append(parameter.getType().asErasure().getTypeName());
        }
        sb.append(')');
        return sb.toString();
    }
}
