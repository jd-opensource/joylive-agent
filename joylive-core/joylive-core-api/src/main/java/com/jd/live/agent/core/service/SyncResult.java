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
package com.jd.live.agent.core.service;

import lombok.Getter;
import lombok.Setter;

/**
 * A generic class that encapsulates a result object along with its associated metadata.
 *
 * @param <T> The type of the data object.
 * @param <M> The type of the metadata object.
 */
@Setter
@Getter
public class SyncResult<T, M> {

    /**
     * The data object of type T.
     */
    private T data;

    /**
     * The metadata object of type M.
     */
    private M meta;

    /**
     * Constructs an empty {@code SyncResult} instance.
     */
    public SyncResult() {
    }

    /**
     * Constructs a {@code SyncResult} instance with the specified data and metadata.
     *
     * @param data The data object to be stored in this {@code SyncResult}.
     * @param meta The metadata object to be associated with the data.
     */
    public SyncResult(T data, M meta) {
        this.data = data;
        this.meta = meta;
    }

}

