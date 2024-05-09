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
package com.jd.live.agent.core.bytekit.matcher;

import com.jd.live.agent.core.bytekit.matcher.MethodTypeMatcher.MethodType;
import com.jd.live.agent.core.bytekit.matcher.ModifierMatcher.Mode;
import com.jd.live.agent.core.bytekit.matcher.StringMatcher.OperationMode;
import com.jd.live.agent.core.bytekit.type.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Provides a set of static factory methods for creating various types of {@link ElementMatcher}s.
 * These matchers are used to specify the criteria for matching elements such as classes,
 * methods, or annotations within the byte code instrumentation process.
 * <p>
 * The {@code MatcherBuilder} class simplifies the creation of complex matching rules by
 * providing a fluent and intuitive API for defining match conditions. This can be particularly
 * useful when writing plugins or rules for bytecode manipulation frameworks.
 *
 * @since 2024-01-21
 */
public class MatcherBuilder {

    private MatcherBuilder() {

    }

    /**
     * Creates a matcher that matches named elements with the given name.
     *
     * @param name The name to match.
     * @param <T>  The type of the named element.
     * @return A matcher that matches elements with the specified name.
     */
    public static <T extends NamedElement> Junction<T> named(String name) {
        return new NameMatcher<>(new StringMatcher(name, OperationMode.EQUALS_FULLY));
    }

    /**
     * Creates a matcher that matches named elements with any of the given names.
     *
     * @param names The names to match.
     * @param <T>   The type of the named element.
     * @return A matcher that matches elements with any of the specified names.
     */
    public static <T extends NamedElement> Junction<T> in(String... names) {
        return new NameMatcher<>(new OneOfMatcher(names));
    }

    /**
     * Creates a matcher that matches elements whose name is in the specified set of names.
     *
     * @param names The set of names to match against.
     * @param <T>   The type of the named element.
     * @return A matcher that matches elements whose name is contained in the specified set.
     */
    public static <T extends NamedElement> Junction<T> in(Set<String> names) {
        return new NameMatcher<>(new OneOfMatcher(names));
    }

    /**
     * Creates a matcher that matches elements that are annotated with the specified annotation.
     *
     * @param annotation The name of the annotation to match.
     * @param <T>        The type of the element that can be annotated.
     * @return A matcher that matches elements annotated with the specified annotation.
     */
    public static <T extends AnnotationSource> Junction<T> isAnnotated(String annotation) {
        return new DeclaringAnnotationMatcher<>(new CollectionItemMatcher<>(
                new AnnotationTypeMatcher<>(new NameMatcher<>(
                        new StringMatcher(annotation, OperationMode.EQUALS_FULLY)))));
    }

    /**
     * Creates a matcher that matches elements that are subtypes of the specified type.
     *
     * @param type The name of the type to match subtypes against.
     * @param <T>  The type of the element.
     * @return A matcher that matches elements that are subtypes of the specified type.
     */
    public static <T extends TypeDesc> Junction<T> isSubTypeOf(String type) {
        return new SubTypeMatcher.SubNameMatcher<>(type);
    }

    /**
     * Creates a matcher that matches elements that are subtypes of the specified class.
     *
     * @param type The class to match subtypes against.
     * @param <T>  The type of the element.
     * @return A matcher that matches elements that are subtypes of the specified class.
     */
    public static <T extends TypeDesc> Junction<T> isSubTypeOf(Class<?> type) {
        return new SubTypeMatcher<>(type);
    }

    /**
     * Creates a matcher that matches elements that implement the specified interface.
     *
     * @param type The name of the interface to match implementors against.
     * @param <T>  The type of the element.
     * @return A matcher that matches elements that implement the specified interface.
     */
    public static <T extends TypeDesc> Junction<T> isImplement(String type) {
        return new SubTypeMatcher.SubNameMatcher<>(type, true);
    }

    /**
     * Creates a matcher that matches elements that implement the specified interface.
     *
     * @param type The class of the interface to match implementors against.
     * @param <T>  The type of the element.
     * @return A matcher that matches elements that implement the specified interface class.
     */
    public static <T extends TypeDesc> Junction<T> isImplement(Class<?> type) {
        return new SubTypeMatcher<>(type, true);
    }

    /**
     * Creates a matcher that matches elements that are super types of the specified class.
     *
     * @param type The class to match super types against.
     * @param <T>  The type of the element.
     * @return A matcher that matches elements that are super types of the specified class.
     */
    public static <T extends TypeDesc> Junction<T> isSuperTypeOf(Class<?> type) {
        return new SuperTypeMatcher<>(type);
    }

    /**
     * Creates a matcher that matches elements that are interfaces.
     *
     * @param <T> The type of the element.
     * @return A matcher that matches interface elements.
     */
    public static <T extends ModifierDesc> Junction<T> isInterface() {
        return ModifierMatcher.of(ModifierMatcher.Mode.INTERFACE);
    }

    /**
     * Creates a matcher that matches elements that are annotations.
     *
     * @param <T> The type of the element.
     * @return A matcher that matches annotation elements.
     */
    public static <T extends ModifierDesc> Junction<T> isAnnotation() {
        return ModifierMatcher.of(ModifierMatcher.Mode.ANNOTATION);
    }

    /**
     * Creates a matcher that matches elements that are constructors.
     *
     * @param <T> The type of the element.
     * @return A matcher that matches constructor elements.
     */
    public static <T extends MethodDesc> Junction<T> isConstructor() {
        return new MethodTypeMatcher<>(MethodType.CONSTRUCTOR);
    }

    /**
     * Creates a matcher that matches elements that are methods (excluding constructors).
     *
     * @param <T> The type of the element.
     * @return A matcher that matches method elements.
     */
    public static <T extends MethodDesc> Junction<T> isMethod() {
        return new MethodTypeMatcher<>(MethodType.METHOD);
    }

    /**
     * Creates a matcher that matches elements that are default methods.
     *
     * @param <T> The type of the element.
     * @return A matcher that matches default method elements.
     */
    public static <T extends MethodDesc> Junction<T> isDefaultMethod() {
        return new MethodTypeMatcher<>(MethodType.DEFAULT_METHOD);
    }

    /**
     * Creates a matcher that matches elements that are static.
     *
     * @param <T> The type of the element.
     * @return A matcher that matches static elements.
     */
    public static <T extends ModifierDesc> Junction<T> isStatic() {
        return ModifierMatcher.of(Mode.STATIC);
    }

    /**
     * Creates a matcher that matches elements that are public.
     *
     * @param <T> The type of the element.
     * @return A matcher that matches public elements.
     */
    public static <T extends ModifierDesc> Junction<T> isPublic() {
        return ModifierMatcher.of(Mode.PUBLIC);
    }

    /**
     * Creates a matcher that matches elements that are protected.
     *
     * @param <T> The type of the element.
     * @return A matcher that matches protected elements.
     */
    public static <T extends ModifierDesc> Junction<T> isProtect() {
        return ModifierMatcher.of(Mode.PROTECTED);
    }

    /**
     * Creates a matcher that matches elements that are private.
     *
     * @param <T> The type of the element.
     * @return A matcher that matches private elements.
     */
    public static <T extends ModifierDesc> Junction<T> isPrivate() {
        return ModifierMatcher.of(Mode.PRIVATE);
    }

    /**
     * Creates a matcher that matches elements that are abstract.
     *
     * @param <T> The type of the element.
     * @return A matcher that matches private elements.
     */
    public static <T extends ModifierDesc> Junction<T> isAbstract() {
        return ModifierMatcher.of(Mode.ABSTRACT);
    }

    /**
     * Creates a matcher that matches methods with the specified parameter types.
     *
     * @param types The names of the parameter types to match.
     * @param <T>   The type of the method.
     * @return A matcher that matches methods with the specified parameter types.
     */
    public static <T extends MethodDesc> Junction<T> arguments(String... types) {
        if (types == null || types.length == 0) {
            return arguments(0);
        } else {
            List<ElementMatcher<ParameterDesc>> matchers = new ArrayList<>(types.length);
            for (String type : types) {
                matchers.add(new ParameterTypeMatcher<>(new NameMatcher<>(new StringMatcher(type, OperationMode.EQUALS_FULLY))));
            }
            return new ParametersMatcher<>(new CollectionOneToOneMatcher<>(matchers));
        }
    }

    /**
     * Creates a matcher that matches methods with the specified parameter types.
     *
     * @param types The parameter type matchers to use for matching.
     * @param <T>   The type of the method.
     * @return A matcher that matches methods with the specified parameter type matchers.
     */
    @SafeVarargs
    public static <T extends MethodDesc> Junction<T> arguments(ElementMatcher<? super TypeDesc>... types) {
        if (types == null || types.length == 0) {
            return arguments(0);
        } else {
            List<ElementMatcher<ParameterDesc>> matchers = new ArrayList<>(types.length);
            for (ElementMatcher<? super TypeDesc> type : types) {
                matchers.add(new ParameterTypeMatcher<>(type));
            }
            return new ParametersMatcher<>(new CollectionOneToOneMatcher<>(matchers));
        }
    }

    /**
     * Creates a matcher that matches methods with the specified parameter types.
     *
     * @param types The parameter classes to match.
     * @param <T>   The type of the method.
     * @return A matcher that matches methods with the specified parameter classes.
     */
    public static <T extends MethodDesc> Junction<T> arguments(Class<?>... types) {
        if (types == null || types.length == 0) {
            return arguments(0);
        } else {
            List<ElementMatcher<ParameterDesc>> matchers = new ArrayList<>(types.length);
            for (Class<?> type : types) {
                matchers.add(new ParameterTypeMatcher<>(new NameMatcher<>(new StringMatcher(type.getName(), OperationMode.EQUALS_FULLY))));
            }
            return new ParametersMatcher<>(new CollectionOneToOneMatcher<>(matchers));
        }
    }

    /**
     * Creates a matcher that matches methods with the specified number of parameters.
     *
     * @param count The number of parameters to match.
     * @param <T>   The type of the method.
     * @return A matcher that matches methods with the specified number of parameters.
     */
    public static <T extends MethodDesc> Junction<T> arguments(int count) {
        return new ParametersMatcher<>(new SizeMatcher<>(count));
    }

    /**
     * Creates a matcher that negates the match result of the given matcher.
     *
     * @param matcher The matcher to negate.
     * @param <T>     The type of the element.
     * @return A matcher that negates the result of the given matcher.
     */
    public static <T> Junction<T> not(ElementMatcher<? super T> matcher) {
        return new NegatingMatcher<>(matcher);
    }

    /**
     * Creates a matcher that matches any element.
     *
     * @param <T> The type of the element.
     * @return A matcher that matches any element.
     */
    public static <T> Junction<T> any() {
        return BooleanMatcher.of(true);
    }

    /**
     * Creates a matcher that matches no elements.
     *
     * @param <T> The type of the element.
     * @return A matcher that matches no elements.
     */
    public static <T> Junction<T> none() {
        return BooleanMatcher.of(false);
    }
}
