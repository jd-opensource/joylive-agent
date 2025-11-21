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
 * Annotation to designate a class as configurable. It signifies that the class instances can have
 * their fields filled with configuration values. This annotation offers options to set a prefix for
 * configuration keys and to enable automatic injection of configuration values into all fields
 * of the class that are eligible.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Configurable {

    /**
     * Specifies the prefix to be used for the configuration keys. The default is an empty string,
     * which usually means that the prefix will be derived from the class name or based on a convention.
     *
     * @return The prefix string for retrieving configuration values.
     */
    String prefix() default "";

    /**
     * Indicates whether configuration values should be automatically injected into all eligible fields
     * of the annotated class. The default value is false, which means that automatic injection is not
     * enabled by default.
     *
     * @return {@code true} if automatic injection is enabled for all fields; {@code false} otherwise.
     */
    boolean auto() default false;
}

