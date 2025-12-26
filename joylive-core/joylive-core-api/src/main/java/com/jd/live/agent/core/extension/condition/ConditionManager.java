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
import com.jd.live.agent.core.extension.annotation.ConditionalComposite;
import com.jd.live.agent.core.extension.condition.Condition.CompositeCondition;
import com.jd.live.agent.core.extension.condition.Condition.DelegateCondition;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private static final Map<Class<?>, Optional<Condition>> CONDITIONS = new ConcurrentHashMap<>();

    private static final Map<Class<?>, List<DelegateCondition>> TYPE_CONDITIONS = new ConcurrentHashMap<>();

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
        classLoader = classLoader == null ? this.classLoader : classLoader;
        List<DelegateCondition> conditions = getConditions(type);
        Annotation annotation;
        for (DelegateCondition condition : conditions) {
            annotation = condition.getAnnotation();
            if (predicate == null || predicate.test(annotation)) {
                ConditionContext context = new ConditionContext(type, annotation, classLoader, optional, this::parseAnnotation);
                if (!condition.match(context)) {
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
    private List<DelegateCondition> getConditions(Class<?> type) {
        // Fix potential deadlock by avoiding computeIfAbsent which holds lock during class loading
        List<DelegateCondition> conditions = TYPE_CONDITIONS.get(type);
        if (conditions != null) {
            return conditions;
        }
        conditions = parseConditions(type);
        List<DelegateCondition> old = TYPE_CONDITIONS.putIfAbsent(type, conditions);
        return old != null ? old : conditions;
    }

    /**
     * Returns a list of delegate conditions for the given type.
     *
     * @param type the type to retrieve delegate conditions for
     * @return a list of delegate conditions for the given type
     */
    private List<DelegateCondition> parseConditions(Class<?> type) {
        List<DelegateCondition> result = new ArrayList<>();
        Annotation[] annotations = type.getAnnotations();
        for (Annotation annotation : annotations) {
            Condition condition = getCondition(annotation.annotationType(), this::parseAnnotation);
            if (condition != null) {
                result.add(new DelegateCondition(annotation, condition));
            }
        }
        return result;
    }

    /**
     * Parses the given annotation type and returns a condition based on its conditional annotations.
     *
     * @param annotationType the annotation type to parse
     * @return a condition based on the conditional annotations of the given annotation type, or null if the annotation
     * type does not have a conditional annotation
     */
    private Condition parseAnnotation(Class<?> annotationType) {
        Conditional conditional = annotationType.getAnnotation(Conditional.class);
        Condition condition = null;
        if (conditional != null) {
            condition = newImplement(conditional.value(), annotationType);
        } else {
            ConditionalComposite composite = annotationType.getAnnotation(ConditionalComposite.class);
            if (composite != null) {
                condition = new CompositeCondition(parseConditions(annotationType));
            }
        }
        return condition;
    }

    /**
     * Returns a condition for the given annotation type.
     * <p>
     * This method uses a cache to store conditions for annotation types. If the condition for the given annotation type
     * is not in the cache, it creates a new condition using the provided function and stores it in the cache. The
     * function takes the annotation type as input and returns a new condition.
     *
     * @param type     the annotation type to retrieve a condition for
     * @param function a function that creates a new condition for the given annotation type
     * @return a condition for the given annotation type, or null if the condition could not be created
     */
    private Condition getCondition(Class<?> type, Function<Class<?>, Condition> function) {
        if (SYSTEM_ANNOTATION.test(type)) {
            return null;
        }
        Optional<Condition> optional = CONDITIONS.get(type);
        if (optional == null) {
            optional = Optional.ofNullable(function.apply(type));
            Optional<Condition> old = CONDITIONS.putIfAbsent(type, optional);
            if (old != null) {
                optional = old;
            }
        }
        return optional.orElse(null);
    }

    /**
     * Instantiates a new condition implementation based on the provided class name and conditional name. This method
     * supports both explicit class names and a naming convention that infers the class name from the conditional
     * annotation name.
     *
     * @param implementClass The class name of the condition implementation.
     * @param annotationType The type of the conditional annotation.
     * @return A new instance of the condition implementation, or null if instantiation fails.
     */
    private Condition newImplement(String implementClass, Class<?> annotationType) {
        String conditionalName = annotationType.getSimpleName();
        String candidateClass = null;
        if (implementClass == null || implementClass.isEmpty()) {
            if (conditionalName.startsWith(CONDITIONAL_PREFIX)) {
                String name = conditionalName.substring(CONDITIONAL_PREFIX.length()) + CONDITION_SUFFIX;
                implementClass = CONDITION_PACKAGE + "." + name;
                candidateClass = annotationType.getPackage().getName() + "." + name;
            }
        }
        if (implementClass != null && !implementClass.isEmpty()) {
            try {
                Class<?> type = classLoader.loadClass(implementClass);
                return (Condition) type.newInstance();
            } catch (Throwable e) {
                if (candidateClass != null) {
                    try {
                        Class<?> type = classLoader.loadClass(candidateClass);
                        return (Condition) type.newInstance();
                    } catch (Throwable e1) {
                        logger.error("failed to instantiate " + conditionalName, e);
                    }
                }
            }
        } else {
            logger.error("failed to instantiate " + conditionalName);
        }
        return null;
    }

}
