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
package com.jd.live.agent.core.inject.annotation;

import java.lang.annotation.*;

/**
 * Annotation to mark a class as eligible for automatic instance injection. This signifies that
 * instances of the class can be automatically created and injected at designated injection points
 * within the application. It provides an option to enable or disable automatic injection for the
 * annotated class.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Injectable {

    /**
     * Specifies whether instances of the annotated class should be automatically created and injected.
     * The default value is true, indicating that automatic injection is enabled by default.
     *
     * @return {@code true} if automatic injection is enabled; {@code false} otherwise.
     */
    boolean enable() default true;
}

