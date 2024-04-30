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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.extension.annotation.Conditional;
import com.jd.live.agent.core.util.cache.LazyObject;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * ConditionManager is a utility class responsible for managing and evaluating conditional logic based on annotations
 * and class types. It implements the {@link ConditionMatcher} interface and provides methods to determine whether a
 * given class matches the specified conditions.
 *
 * <p>This class uses a combination of static maps to cache conditions and their descriptors, which are used to
 * efficiently evaluate the match criteria. It also supports lazy initialization of condition implementations to
 * minimize the performance impact of condition checking.</p>
 *
 * @since 1.0.0
 */
public class ConditionManager implements ConditionMatcher {

    private static final Logger logger = LoggerFactory.getLogger(ConditionManager.class);

    private static final String CONDITIONAL_PREFIX = "Conditional";

    private static final String CONDITION_SUFFIX = "Condition";

    private static final String CONDITION_PACKAGE = ConditionManager.class.getPackage().getName();

    private static final Map<Class<?>, LazyObject<Condition>> CONDITIONS = new ConcurrentHashMap<>();

    private static final Map<Class<?>, List<ConditionalDesc>> TYPE_CONDITIONS = new ConcurrentHashMap<>();

    private final ClassLoader classLoader;

    private final Function<String, String> optional;

    private final Predicate<Annotation> predicate;

    public ConditionManager(ClassLoader classLoader, Function<String, String> optional, Predicate<Annotation> predicate) {
        this.classLoader = classLoader;
        this.optional = optional;
        this.predicate = predicate;
    }

    @Override
    public boolean match(Class<?> type) {
        return match(type, classLoader, predicate);
    }

    @Override
    public boolean match(Class<?> type, ClassLoader classLoader) {
        return match(type, classLoader, predicate);
    }

    @Override
    public boolean match(Class<?> type, ClassLoader classLoader, Predicate<Annotation> predicate) {
        if (type == null) {
            return false;
        }
        List<ConditionalDesc> descs = TYPE_CONDITIONS.computeIfAbsent(type, this::getConditionals);
        for (ConditionalDesc desc : descs) {
            if (predicate == null || predicate.test(desc.annotation)) {
                if (desc.condition == null || !desc.condition.match(
                        new ConditionContext(type, desc.annotation,
                                classLoader == null ? this.classLoader : classLoader, optional))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Retrieves the list of conditional descriptors for the given class type. This method is called internally to
     * cache and organize the conditions associated with a specific class.
     *
     * @param type The class type to retrieve conditions for.
     * @return A list of conditional descriptors.
     */
    private List<ConditionalDesc> getConditionals(Class<?> type) {
        List<ConditionalDesc> result = new ArrayList<>();
        Annotation[] annotations = type.getAnnotations();
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            Conditional conditional = annotationType.getAnnotation(Conditional.class);
            if (conditional != null) {
                LazyObject<Condition> lazyObject = CONDITIONS.computeIfAbsent(annotationType,
                        t -> new LazyObject<>(newImplement(conditional.value(), t.getSimpleName())));
                result.add(new ConditionalDesc(annotation, lazyObject.get()));
            }
        }
        return result;
    }

    /**
     * Instantiates a new condition implementation based on the provided class name and conditional name. This method
     * supports both explicit class names and a naming convention that infers the class name from the conditional
     * annotation name.
     *
     * @param implementClass The class name of the condition implementation.
     * @param conditionalName The name of the conditional annotation.
     * @return A new instance of the condition implementation, or null if instantiation fails.
     */
    private Condition newImplement(String implementClass, String conditionalName) {
        if (implementClass == null || implementClass.isEmpty()) {
            if (conditionalName.startsWith(CONDITIONAL_PREFIX)) {
                implementClass = CONDITION_PACKAGE + "." + conditionalName.substring(CONDITIONAL_PREFIX.length()) + CONDITION_SUFFIX;
            }
        }
        if (implementClass != null && !implementClass.isEmpty()) {
            try {
                Class<?> type = classLoader.loadClass(implementClass);
                return (Condition) type.newInstance();
            } catch (Throwable e) {
                logger.error("failed to instantiate " + conditionalName, e);
            }
        } else {
            logger.error("failed to instantiate " + conditionalName);
        }
        return null;
    }

    /**
     * A private static class that serves as a container for holding a conditional annotation and its associated
     * condition implementation. Each instance of this class represents a single conditional that is to be checked
     * against a class type.
     *
     * <p>This class is used internally by the {@link ConditionManager} to cache and manage the relationship between
     * annotations and their corresponding conditions, enabling efficient conditional matching.</p>
     */
    private static class ConditionalDesc {

        protected final Annotation annotation;

        protected final Condition condition;

        ConditionalDesc(Annotation annotation, Condition condition) {
            this.annotation = annotation;
            this.condition = condition;
        }
    }

}
