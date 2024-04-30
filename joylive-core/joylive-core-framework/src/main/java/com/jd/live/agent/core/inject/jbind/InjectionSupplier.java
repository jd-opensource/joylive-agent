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
package com.jd.live.agent.core.inject.jbind;

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.core.inject.Injection;

/**
 * The InjectionSupplier interface provides a contract for supplying instances of objects through dependency injection.
 * It allows for the creation of objects based on their type and the context in which they are to be used.
 *
 * @since 1.0.0
 */
@Extensible("InjectionSupplier")
public interface InjectionSupplier {

    /**
     * A constant representing the annotation that indicates a configuration supplier.
     */
    String CONFIG_ANNOTATION_SUPPLIER = "Config";

    /**
     * A constant representing the annotation that indicates a general injection supplier.
     */
    String INJECT_ANNOTATION_SUPPLIER = "Inject";

    /**
     * Builds an instance of the specified type using the given injection context.
     * This method is responsible for handling the instantiation and injection of dependencies
     * for the requested type, taking into account any specific requirements or behaviors
     * defined by the injection context.
     *
     * @param type the Class object representing the type of object to build
     * @param context the InjectionContext that provides the context for the injection
     * @return an instance of the specified type, with its dependencies injected
     */
    Injection build(Class<?> type, InjectionContext context);
}

