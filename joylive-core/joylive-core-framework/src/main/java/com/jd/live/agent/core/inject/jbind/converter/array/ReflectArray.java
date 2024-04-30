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

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.jbind.ArrayObject;

import java.lang.reflect.Array;

@Extension(value = "ReflectArray", order = Integer.MAX_VALUE)
public class ReflectArray implements ArrayObject {

    protected Object array;
    protected int size;

    public ReflectArray(Object array) {
        this.array = array;
        this.size = array == null ? 0 : Array.getLength(array);
    }

    @Override
    public Object get(int index) {
        if (index >= size)
            throw new ArrayIndexOutOfBoundsException();
        return Array.get(array, index);
    }

    @Override
    public void set(int index, Object item) {
        if (index < 0 && index >= size)
            throw new ArrayIndexOutOfBoundsException();
        Array.set(array, index, item);
    }

    @Override
    public int length() {
        return size;
    }

    @Override
    public Object array() {
        return array;
    }
}
