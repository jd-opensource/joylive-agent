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
package com.jd.live.agent.core.util.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Functional interface for retrieving annotations from different sources.
 */
@FunctionalInterface
public interface AnnotationGetter {
    /**
     * Gets annotation of specified type from the source.
     *
     * @param annotationClass the Class object of the annotation type
     * @return the annotation if present, else null
     */
    <A extends Annotation> A getAnnotation(Class<A> annotationClass);

    /**
     * Implementation for getting annotations from a Method.
     */
    class MethodAnnotationGetter implements AnnotationGetter {
        private final Method method;

        public MethodAnnotationGetter(Method method) {
            this.method = method;
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
            return method.getAnnotation(annotationClass);
        }
    }

    /**
     * Implementation for getting annotations from a Parameter.
     */
    class ParameterAnnotationGetter implements AnnotationGetter {

        private final Parameter parameter;

        public ParameterAnnotationGetter(Parameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
            return parameter.getAnnotation(annotationClass);
        }
    }

    /**
     * Implementation for getting annotations from a Class type.
     */
    class TypeAnnotationGetter implements AnnotationGetter {
        private final Class<?> type;

        public TypeAnnotationGetter(Class<?> type) {
            this.type = type;
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
            return type.getAnnotation(annotationClass);
        }
    }
}
