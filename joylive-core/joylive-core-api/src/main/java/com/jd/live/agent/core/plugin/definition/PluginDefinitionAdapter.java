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
package com.jd.live.agent.core.plugin.definition;

import com.jd.live.agent.core.bytekit.matcher.ElementMatcher;
import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.bytekit.type.TypeDesc;

import java.util.function.Supplier;

/**
 * An adapter implementation of the {@link PluginDefinition} interface, providing
 * a flexible way to define the behavior of plugins through constructors.
 * <p>
 * This class allows for the specification of the plugin's matching criteria and
 * the interceptors to be applied when the criteria are met. It supports different
 * ways of specifying the matching criteria, including using type names, matcher
 * instances, or a supplier for lazy evaluation.
 * <p>
 * This adapter makes it easier to create plugin definitions without needing to
 * implement the {@link PluginDefinition} interface directly every time.
 *
 * @author Zhiguo.Chen
 * @since 2024-01-20
 */
public class PluginDefinitionAdapter implements PluginDefinition {

    /**
     * A supplier that provides the {@link ElementMatcher} used to determine if
     * the plugin should be applied to a given class.
     */
    protected Supplier<ElementMatcher<TypeDesc>> matcher;

    /**
     * An array of {@link InterceptorDefinition}s that define the interceptors
     * to be applied by this plugin.
     */
    protected InterceptorDefinition[] interceptors;

    /**
     * Protected constructor for subclassing.
     */
    protected PluginDefinitionAdapter() {
    }

    /**
     * Constructs a new {@code PluginDefinitionAdapter} with a type name for
     * matching and interceptors to apply.
     *
     * @param type         The name of the type to match.
     * @param interceptors The interceptors to apply.
     */
    public PluginDefinitionAdapter(String type, InterceptorDefinition... interceptors) {
        this(() -> MatcherBuilder.named(type), interceptors);
    }

    /**
     * Constructs a new {@code PluginDefinitionAdapter} with a matcher and
     * interceptors to apply.
     *
     * @param matcher      The matcher to determine if the plugin should be applied.
     * @param interceptors The interceptors to apply.
     */
    public PluginDefinitionAdapter(ElementMatcher<TypeDesc> matcher, InterceptorDefinition... interceptors) {
        this(() -> matcher, interceptors);
    }

    /**
     * Constructs a new {@code PluginDefinitionAdapter} with a supplier that provides
     * the matcher and interceptors to apply.
     *
     * @param matcher      A supplier that provides the matcher.
     * @param interceptors The interceptors to apply.
     */
    public PluginDefinitionAdapter(Supplier<ElementMatcher<TypeDesc>> matcher, InterceptorDefinition[] interceptors) {
        this.matcher = matcher;
        this.interceptors = interceptors;
    }

    /**
     * Returns the matcher used to determine if this plugin should be applied to a given class.
     *
     * @return The element matcher that defines the criteria for matching classes.
     */
    @Override
    public ElementMatcher<TypeDesc> getMatcher() {
        return matcher.get();
    }

    /**
     * Returns an array of interceptor definitions that should be used to modify
     * or extend the behavior of the matched classes.
     *
     * @return An array of interceptor definitions for this plugin.
     */
    @Override
    public InterceptorDefinition[] getInterceptors() {
        return interceptors;
    }
}
