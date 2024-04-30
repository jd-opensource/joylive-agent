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
package com.jd.live.agent.core.inject.jbind.converter.array;

import com.jd.live.agent.core.inject.jbind.ArrayObject;

public abstract class AbstractObjectArray<T> implements ArrayObject {

    protected T[] array;

    @Override
    public Object get(int index) {
        return array[index];
    }

    @Override
    public void set(int index, Object item) {
        array[index] = (T) item;
    }

    @Override
    public int length() {
        return array.length;
    }

    @Override
    public Object array() {
        return array;
    }
}
