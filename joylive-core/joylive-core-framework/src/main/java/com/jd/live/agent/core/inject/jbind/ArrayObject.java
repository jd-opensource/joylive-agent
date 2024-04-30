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

import java.util.Iterator;

/**
 * The ArrayObject interface provides a generic array-like structure with additional methods
 * for accessing and manipulating its elements.
 */
public interface ArrayObject {

    /**
     * Retrieves the element at the specified index in this array.
     *
     * @param index the index of the element to return
     * @return the element at the specified index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    Object get(int index);

    /**
     * Replaces the element at the specified index with the specified item.
     *
     * @param index   the index of the element to replace
     * @param item    the new item to be stored at the specified index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    void set(int index, Object item);

    /**
     * Returns the length of this array.
     *
     * @return the length of the array
     */
    int length();

    /**
     * Returns the underlying array of this ArrayObject.
     *
     * @return the underlying array
     */
    Object array();

    /**
     * The ArrayObjectIterator class provides an iterator for traversing the elements of an ArrayObject.
     */
    class ArrayObjectIterator implements Iterator<Object> {

        private final ArrayObject target;

        private int index = 0;

        public ArrayObjectIterator(ArrayObject target) {
            this.target = target;
        }

        @Override
        public boolean hasNext() {
            return index < target.length();
        }

        @Override
        public Object next() {
            return target.get(index++);
        }
    }

}
