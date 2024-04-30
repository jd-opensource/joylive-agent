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
package com.jd.live.agent.governance.request;

import lombok.Getter;

/**
 * Represents a simple HTTP cookie with a name and a value.
 * <p>
 * This class encapsulates the properties of an HTTP cookie, providing access to its name and value.
 * It overrides {@code equals()}, {@code hashCode()}, and {@code toString()} methods to facilitate
 * usage in collections and for debugging purposes.
 * </p>
 */
@Getter
public class Cookie {

    /**
     * The name of the cookie.
     */
    private final String name;

    /**
     * The value of the cookie.
     */
    private final String value;

    /**
     * Constructs a new {@code Cookie} instance with the specified name and value.
     * <p>
     * If the provided value is {@code null}, it will be replaced with an empty string to ensure
     * non-null values for all cookie instances.
     * </p>
     *
     * @param name  The name of the cookie.
     * @param value The value of the cookie, or an empty string if {@code null} is provided.
     */
    public Cookie(String name, String value) {
        this.name = name;
        this.value = (value != null ? value : "");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Cookie)) {
            return false;
        }
        return (name.equalsIgnoreCase(((Cookie) other).getName()));
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name + '=' + value;
    }
}
