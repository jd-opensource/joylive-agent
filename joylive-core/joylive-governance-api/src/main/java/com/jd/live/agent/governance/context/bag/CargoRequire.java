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
package com.jd.live.agent.governance.context.bag;

import com.jd.live.agent.core.extension.annotation.Extensible;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Defines a requirement that specifies which cargos should be included based on their names or prefixes.
 */
@Extensible("CargoRequire")
public interface CargoRequire extends Predicate<String> {

    /**
     * Returns an array of exact names that meet the requirement.
     *
     * @return An array of names that should be included.
     */
    Set<String> getNames();

    /**
     * Returns an array of prefixes for names that meet the requirement.
     *
     * @return An array of name prefixes that should be included.
     */
    Set<String> getPrefixes();

    /**
     * Determines if a given name matches the requirement based on the specified names and prefixes.
     *
     * @param name The name to check against the requirement.
     * @return {@code true} if the name matches the requirement; {@code false} otherwise.
     */
    boolean test(String name);

}
