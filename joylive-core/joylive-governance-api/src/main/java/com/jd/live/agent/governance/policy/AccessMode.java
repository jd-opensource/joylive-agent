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
package com.jd.live.agent.governance.policy;

import com.jd.live.agent.core.parser.json.JsonAlias;

/**
 * Enumerates the access modes for resources.
 * <p>
 * This enum defines different access levels for resources, such as whether they can be read, written, or neither.
 * The {@link JsonAlias} annotation specifies the alias used during JSON serialization and deserialization
 * for each enum value.
 * </p>
 */
public enum AccessMode {

    /**
     * Represents the access mode where both reading and writing to the resource are allowed.
     */
    @JsonAlias("ReadWrite")
    READ_WRITE {
        @Override
        public boolean isReadable() {
            return true;
        }

        @Override
        public boolean isWriteable() {
            return true;
        }
    },

    /**
     * Represents the access mode where only reading from the resource is allowed.
     */
    @JsonAlias("Read")
    READ {
        @Override
        public boolean isReadable() {
            return true;
        }

        @Override
        public boolean isWriteable() {
            return false;
        }
    },

    /**
     * Represents the access mode where neither reading from nor writing to the resource is allowed.
     */
    @JsonAlias("None")
    NONE {
        @Override
        public boolean isReadable() {
            return false;
        }

        @Override
        public boolean isWriteable() {
            return false;
        }
    };

    /**
     * Determines if the resource is readable in this access mode.
     *
     * @return {@code true} if the resource is readable; {@code false} otherwise.
     */
    public abstract boolean isReadable();

    /**
     * Determines if the resource is writable in this access mode.
     *
     * @return {@code true} if the resource is writable; {@code false} otherwise.
     */
    public abstract boolean isWriteable();
}
