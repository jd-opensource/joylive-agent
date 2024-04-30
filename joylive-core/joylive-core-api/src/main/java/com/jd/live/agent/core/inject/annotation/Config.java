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
 * Annotation for specifying configuration details for a field. It defines the configuration key
 * for a field and whether the configuration is optional. This annotation can be used to
 * automatically load configuration values into fields at runtime, with support for specifying
 * if a missing configuration (no config) is permissible.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Config {

    /**
     * Specifies the configuration key associated with this field. The default is an empty string,
     * which often means the field's name or some convention-based key will be used to fetch the
     * configuration value.
     *
     * @return The key used to retrieve the configuration value.
     */
    String value() default "";

    /**
     * Indicates whether the configuration for the field is optional. If true, the system will
     * allow for the configuration to be missing without causing errors. The default is true,
     * allowing for optional configuration.
     *
     * @return {@code true} if the field can be left without configuration; {@code false} otherwise.
     */
    boolean nullable() default true;
}

