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
package com.jd.live.agent.core.extension.condition;

import com.jd.live.agent.core.extension.annotation.Conditional;
import com.jd.live.agent.core.extension.annotation.ConditionalComposite;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.function.Predicate;

/**
 * ConditionMatcher is an interface that defines a contract for matching a given class based on certain conditions.
 * It is typically used in frameworks or dependency injection systems to determine if a component should be loaded
 * or instantiated based on its annotations or other criteria.
 */
public interface ConditionMatcher {

    /**
     * A default Predicate that checks if an annotation is system annotation.
     */
    Predicate<Class<?>> SYSTEM_ANNOTATION = type -> type == Conditional.class
            || type == ConditionalComposite.class
            || type == Extension.class
            || type == Injectable.class
            || type.getName().startsWith("java.");

    /**
     * A default Predicate that checks if an annotation is annotated with the {@link Conditional} annotation
     * and if the {@code dependOnLoader} attribute is set to true.
     */
    Predicate<Annotation> DEPEND_ON_LOADER = a -> {
        LinkedList<Annotation> stack = new LinkedList<>();
        stack.push(a);
        while (!stack.isEmpty()) {
            Annotation annotation = stack.pop();
            Class<? extends Annotation> type = annotation.annotationType();
            Conditional conditional = type.getAnnotation(Conditional.class);
            if (conditional != null) {
                if (conditional.dependOnLoader()) {
                    return true;
                }
            } else {
                ConditionalComposite composite = type.getAnnotation(ConditionalComposite.class);
                if (composite != null) {
                    Annotation[] annotations = type.getAnnotations();
                    for (Annotation an : annotations) {
                        Class<? extends Annotation> annotationType = an.annotationType();
                        if (!SYSTEM_ANNOTATION.test(annotationType) && (
                                annotationType.getAnnotation(Conditional.class) != null
                                        || annotationType.getAnnotation(ConditionalComposite.class) != null)) {
                            stack.push(an);
                        }
                    }
                }
            }
        }
        return false;
    };

    /**
     * A constant string used as a key for identifying the condition matcher in a context where multiple matchers
     * may be used or configured.
     */
    String COMPONENT_CONDITION_MATCHER = "conditionMatcher";

    /**
     * Checks if the given class matches the conditions for loading or instantiation.
     * This default method provides an overload that calls the main match method with null values for the
     * class loader and predicate.
     *
     * @param type the class to check for matching conditions.
     * @return true if the class matches the conditions, false otherwise.
     */
    default boolean match(Class<?> type) {
        return match(type, null, null);
    }

    /**
     * Checks if the given class matches the conditions for loading or instantiation, considering the provided
     * class loader.
     * This default method provides an overload that calls the main match method with a null predicate.
     *
     * @param type        the class to check for matching conditions.
     * @param classLoader the class loader to use for loading the class or its dependencies.
     * @return true if the class matches the conditions, false otherwise.
     */
    default boolean match(Class<?> type, ClassLoader classLoader) {
        return match(type, classLoader, null);
    }

    /**
     * Checks if the given class matches the conditions for loading or instantiation, considering the provided
     * class loader and an additional predicate for custom conditions.
     *
     * @param type       the class to check for matching conditions.
     * @param classLoader the class loader to use for loading the class or its dependencies.
     * @param predicate  an additional predicate to apply to the class's annotations for custom matching logic.
     * @return true if the class matches the conditions, false otherwise.
     */
    boolean match(Class<?> type, ClassLoader classLoader, Predicate<Annotation> predicate);
}

