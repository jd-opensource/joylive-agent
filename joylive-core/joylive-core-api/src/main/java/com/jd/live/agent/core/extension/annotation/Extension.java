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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that marks an implementation as an extension. This can be used to mark classes that extend or
 * provide specific implementations of extensible types or interfaces. The {@code value} attribute specifies the
 * type (or name) of the extension, {@code provider} denotes the vendor or creator of the extension, {@code order}
 * is used to specify the loading or execution priority of multiple extensions (with lower values having higher priority),
 * and {@code singleton} indicates whether a single instance of the extension should be used or a new instance can be
 * created each time it is needed.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Extension {
    /**
     * Specifies the type (or name) of the extension. This can be used to identify or categorize the extension.
     *
     * @return The type (or name) of the extension.
     */
    String[] value() default "";

    /**
     * Specifies the provider or vendor of the extension. This can be used for informational purposes or to distinguish
     * between extensions from different sources.
     *
     * @return The provider of the extension.
     */
    String provider() default "";

    /**
     * Specifies the order of the extension in terms of loading or execution priority. Extensions with lower order
     * values have higher priority. The default order is set to {@code Short.MAX_VALUE} to place it at the end of the
     * order unless specified otherwise.
     *
     * @return The order priority of the extension.
     */
    int order() default Short.MAX_VALUE;

    /**
     * Indicates whether the extension should be treated as a singleton. If {@code true}, a single instance of the
     * extension will be used. If {@code false}, new instances of the extension can be created as needed.
     *
     * @return The singleton indicator of the extension.
     */
    boolean singleton() default true;

}
