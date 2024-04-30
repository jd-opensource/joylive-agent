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
import com.jd.live.agent.core.bytekit.type.MethodDesc;

/**
 * Defines a contract for specifying interceptors and their matching conditions.
 * This interface is part of an AOP (Aspect-Oriented Programming) framework where
 * interceptors are used to define cross-cutting concerns such as logging, transaction management, etc.
 *
 * @since 1.0.0
 */
public interface InterceptorDefinition {

    /**
     * Returns the matcher that determines which methods the interceptor applies to.
     * The matcher uses criteria defined in {@link ElementMatcher} to match methods.
     *
     * @return An instance of {@link ElementMatcher} that defines the matching logic for methods.
     */
    ElementMatcher<MethodDesc> getMatcher();

    /**
     * Returns the interceptor instance that will be applied to matched methods.
     * The interceptor defines the behavior that is to be executed before, after, or around the matched method.
     *
     * @return An instance of {@link Interceptor} that encapsulates the interception logic.
     */
    Interceptor getInterceptor();
}
