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
package com.jd.live.agent.core.extension.annotation;

import java.lang.annotation.*;

/**
 * An annotation that indicates a condition based on the absence of a specific class. If the
 * class specified by the {@code value} is not present or cannot be loaded, then the condition
 * is considered to be met. This annotation is often used in configurations to provide fallback
 * functionality or alternate beans when certain classes are not available in the classpath.
 * <p>
 * The annotation is marked with {@code @Conditional(dependOnLoader = true)}, which indicates
 * that the condition depends on the class loader's ability to load the specified class.
 *
 * @since 1.0.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(dependOnLoader = true)
public @interface ConditionalOnMissingClass {

    /**
     * Specifies the fully qualified name of the class that must be absent for the condition
     * to be met. If the specified class cannot be loaded, then the annotated element is
     * considered eligible for whatever operation or processing is being conditionally controlled.
     *
     * @return The fully qualified name of the class that must be missing.
     */
    String value();

}

