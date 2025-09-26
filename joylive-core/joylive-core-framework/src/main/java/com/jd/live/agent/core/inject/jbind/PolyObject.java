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

import java.util.Collection;
import java.util.Iterator;

/**
 * An interface designed for operations that involve collections and arrays, enabling the transformation
 * and manipulation of multiple elements.
 */
public interface PolyObject {

    /**
     * Adds an object to the collection or array.
     *
     * @param obj The object to be added.
     */
    void add(Object obj);

    /**
     * Returns the size of the collection or array.
     *
     * @return The number of elements in the collection or array.
     */
    int size();

    /**
     * Returns an iterator for the elements in the collection or array.
     *
     * @return An Iterator over the elements.
     */
    Iterator<Object> iterator();

    /**
     * Returns the underlying target collection or array.
     *
     * @return The target collection or array.
     */
    Object getTarget();

    /**
     * A PolyObject implementation for collections.
     */
    class CollectionPolyObject implements PolyObject {
        private Collection<Object> target;

        public CollectionPolyObject(Collection<Object> target) {
            this.target = target;
        }

        @Override
        public void add(Object obj) {
            target.add(obj);
        }

        @Override
        public int size() {
            return target.size();
        }

        @Override
        public Iterator<Object> iterator() {
            return target.iterator();
        }

        @Override
        public Object getTarget() {
            return target;
        }
    }

    /**
     * A PolyObject implementation for arrays.
     */
    class ArrayPolyObject implements PolyObject {

        private ArrayObject target;
        private int index = 0;

        public ArrayPolyObject(ArrayObject target) {
            this.target = target;
        }

        @Override
        public void add(Object obj) {
            target.set(index++, obj);
        }

        @Override
        public int size() {
            return target.length();
        }

        @Override
        public Iterator<Object> iterator() {
            return new ArrayObject.ArrayObjectIterator(target);
        }

        @Override
        public Object getTarget() {
            return target.array();
        }
    }
}

