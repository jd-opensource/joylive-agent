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

import com.jd.live.agent.bootstrap.plugin.definition.Interceptor;
import com.jd.live.agent.core.bytekit.matcher.ElementMatcher;
import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.bytekit.type.MethodDesc;

import java.util.function.Supplier;

/**
 * An adapter class that implements {@link InterceptorDefinition} to provide a flexible way of defining
 * method matchers and interceptors. This allows for dynamic creation of interceptors and matchers based on
 * various conditions such as method names, method signatures, etc.
 */
public class InterceptorDefinitionAdapter implements InterceptorDefinition {
    /**
     * The {@link ElementMatcher} used to match methods that the interceptor will apply to.
     */
    private final ElementMatcher<MethodDesc> matcher;

    /**
     * A {@link Supplier} that provides instances of {@link Interceptor}. This allows for lazy
     * instantiation and potentially dynamic selection of interceptors.
     */
    private final Supplier<Interceptor> interceptorSupplier;

    /**
     * Constructs an adapter with a specified method name and a static interceptor.
     *
     * @param methodName  The name of the method to match.
     * @param interceptor The interceptor to be applied to matched methods.
     */
    public InterceptorDefinitionAdapter(String methodName, Interceptor interceptor) {
        this(MatcherBuilder.named(methodName), () -> interceptor);
    }

    /**
     * Constructs an adapter with a specified method name and a supplier for interceptors.
     *
     * @param methodName          The name of the method to match.
     * @param interceptorSupplier A supplier that provides interceptors.
     */
    public InterceptorDefinitionAdapter(String methodName, Supplier<Interceptor> interceptorSupplier) {
        this(MatcherBuilder.named(methodName), interceptorSupplier);
    }

    /**
     * Constructs an adapter with a specified matcher and a static interceptor.
     *
     * @param matcher     The matcher to use for determining applicable methods.
     * @param interceptor The interceptor to be applied to matched methods.
     */
    public InterceptorDefinitionAdapter(ElementMatcher<MethodDesc> matcher, Interceptor interceptor) {
        this(matcher, () -> interceptor);
    }

    /**
     * Constructs an adapter with a specified matcher and a supplier for interceptors.
     *
     * @param matcher             The matcher to use for determining applicable methods.
     * @param interceptorSupplier A supplier that provides interceptors.
     */
    public InterceptorDefinitionAdapter(ElementMatcher<MethodDesc> matcher, Supplier<Interceptor> interceptorSupplier) {
        this.matcher = matcher;
        this.interceptorSupplier = interceptorSupplier;
    }

    /**
     * Returns the method matcher for this interceptor definition.
     *
     * @return The {@link ElementMatcher} used to match methods.
     */
    @Override
    public ElementMatcher<MethodDesc> getMatcher() {
        return matcher;
    }

    /**
     * Returns the interceptor for this definition.
     *
     * @return The {@link Interceptor} to be applied to matched methods.
     */
    @Override
    public Interceptor getInterceptor() {
        return interceptorSupplier.get();
    }
}

