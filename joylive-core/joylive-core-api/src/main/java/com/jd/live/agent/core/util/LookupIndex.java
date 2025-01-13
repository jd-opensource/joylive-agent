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
package com.jd.live.agent.core.util;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a lookup index that can store a single index or a list of indices.
 */
@Getter
public class LookupIndex {

    /**
     * The single index value, initialized to -1 to indicate no index is set.
     */
    private int index = -1;

    /**
     * The list of indices, initialized to null.
     */
    private List<Integer> indices;

    /**
     * Adds an index to the lookup index.
     * If no index is currently set, it initializes the single index with the provided value.
     * If a single index is already set, it initializes the list of indices and adds both the current and new indices.
     * Otherwise, it simply adds the new index to the list.
     *
     * @param index the index to add
     */
    public void add(int index) {
        if (this.index < 0) {
            this.index = index;
        } else if (indices == null) {
            indices = new ArrayList<>(2);
            indices.add(this.index);
            indices.add(index);
        } else {
            indices.add(index);
        }
    }

    /**
     * Returns the number of indices stored in the lookup index.
     * If there are multiple indices, it returns the size of the list.
     * If there is a single index, it returns 1.
     * If no indices are set, it returns 0.
     *
     * @return the number of indices stored
     */
    public int size() {
        if (indices != null) {
            return indices.size();
        } else if (index >= 0) {
            return 1;
        }
        return 0;
    }

}
