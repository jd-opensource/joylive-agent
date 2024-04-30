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

/**
 * Defines a requirement that specifies which cargos should be included based on their names or prefixes.
 * <p>
 * This interface allows for the dynamic inclusion of cargos by specifying exact names or name prefixes that match certain
 * criteria. Implementations can provide the specific names and prefixes that meet the requirement. The {@code match} method
 * is used to determine if a given name satisfies the requirement based on these names and prefixes.
 * </p>
 * <p>
 * The {@code @Extensible} annotation indicates that this interface is intended to be extended or implemented by other classes,
 * potentially allowing for custom implementations of cargo requirements.
 * </p>
 */
@Extensible("CargoRequire")
public interface CargoRequire {

    /**
     * Returns an array of exact names that meet the requirement.
     *
     * @return An array of names that should be included.
     */
    String[] getNames();

    /**
     * Returns an array of prefixes for names that meet the requirement.
     * <p>
     * By default, this method returns an empty array, indicating no prefixes are defined.
     * </p>
     *
     * @return An array of name prefixes that should be included.
     */
    default String[] getPrefixes() {
        return new String[0];
    }

    /**
     * Determines if a given name matches the requirement based on the specified names and prefixes.
     * <p>
     * A name matches the requirement if it is exactly equal to one of the specified names or if it starts with one of the
     * specified prefixes.
     * </p>
     *
     * @param name The name to check against the requirement.
     * @return {@code true} if the name matches the requirement; {@code false} otherwise.
     */
    default boolean match(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        String[] names = getNames();
        if (names != null) {
            for (String v : names) {
                if (v.equals(name))
                    return true;
            }
        }
        String[] prefixes = getPrefixes();
        if (prefixes != null) {
            for (String v : prefixes) {
                if (name.startsWith(v))
                    return true;
            }
        }
        return false;
    }

}
