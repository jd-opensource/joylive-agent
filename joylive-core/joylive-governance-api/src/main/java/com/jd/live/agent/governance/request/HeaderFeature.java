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
 * An interface representing a feature of a header, providing methods to determine if the header is duplicable
 * and if it is an array.
 */
public interface HeaderFeature {

    HeaderFeature DEFAULT = new DefaultFeature(false, false);

    HeaderFeature DUPLICABLE = new DefaultFeature(true, false);

    HeaderFeature BATCHABLE = new DefaultFeature(false, true);

    HeaderFeature DUPLICABLE_BATCHABLE = new DefaultFeature(true, true);

    /**
     * Checks if the header is duplicable.
     * By default, this method returns {@code false}, indicating that the header is not duplicable.
     *
     * @return {@code true} if the header is duplicable, {@code false} otherwise
     */
    boolean isDuplicable();

    /**
     * Checks if the header is an array.
     *
     * @return {@code true} if the header is an array, {@code false} otherwise
     */
    boolean isBatchable();

    /**
     * A default implementation of the {@link HeaderFeature} interface.
     */
    @Getter
    class DefaultFeature implements HeaderFeature {

        private final boolean duplicable;

        private final boolean batchable;

        public DefaultFeature(boolean duplicable) {
            this(duplicable, false);
        }

        public DefaultFeature(boolean duplicable, boolean batchable) {
            this.duplicable = duplicable;
            this.batchable = batchable;
        }

    }
}
