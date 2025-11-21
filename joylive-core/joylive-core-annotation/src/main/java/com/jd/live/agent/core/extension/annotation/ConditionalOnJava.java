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
 * An annotation that indicates a condition based on the Java version. The condition
 * is considered met if the current Java runtime version matches the specified version
 * expression. This can be useful for conditionally enabling or disabling certain
 * functionality based on the Java version, ensuring compatibility or leveraging
 * version-specific features.
 * <p>
 * This annotation uses the {@code @Conditional} annotation to specify that the
 * conditionality is determined by the Java version. The {@code value} attribute
 * should contain a version expression that can be used to match against the
 * current Java version.
 *
 * @since 1.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional
public @interface ConditionalOnJava {

    /**
     * Specifies the version expression that determines the Java version condition.
     * The expression format and how it is evaluated can depend on the implementation,
     * but it typically allows for specifying a version range or a specific version.
     *
     * @return The version expression against which the current Java runtime version
     * will be matched.
     */
    String value();

}

