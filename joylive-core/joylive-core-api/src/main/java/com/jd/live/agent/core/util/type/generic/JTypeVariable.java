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
package com.jd.live.agent.core.util.type.generic;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

public class JTypeVariable<D extends GenericDeclaration> implements TypeVariable<D> {

    protected final TypeVariable<D> source;

    protected final Type[] bounds;

    public JTypeVariable(TypeVariable<D> source, Type[] bounds) {
        this.source = source;
        this.bounds = bounds;
    }

    @Override
    public Type[] getBounds() {
        return bounds;
    }

    @Override
    public D getGenericDeclaration() {
        return source.getGenericDeclaration();
    }

    @Override
    public String getName() {
        return source.getName();
    }

    @Override
    public AnnotatedType[] getAnnotatedBounds() {
        return source.getAnnotatedBounds();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return source.getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return source.getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return source.getDeclaredAnnotations();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JTypeVariable<?> that = (JTypeVariable<?>) o;

        return source.equals(that.source);
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }
}
