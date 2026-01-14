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
package com.jd.live.agent.implement.event.opentelemetry.metric;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;

import java.util.function.Predicate;

public class ArrayAttributesBuilder implements AttributesBuilder {
    private Object[] data;

    private int length;

    ArrayAttributesBuilder(Object[] data) {
        this.data = data;
        this.length = data.length;
    }

    public ArrayAttributesBuilder(int initialCapacity) {
        this.data = new Object[initialCapacity % 2 == 0 ? initialCapacity : initialCapacity + 1];
        this.length = 0;
    }

    @Override
    public Attributes build() {
        if (length == 0) {
            return ArrayAttributes.EMPTY;
        } else if (length == data.length) {
            // If only one key-value pair AND the entry hasn't been set to null (by #remove(AttributeKey<T>)
            // or #removeIf(Predicate<AttributeKey<?>>)), then we can bypass sorting and filtering
            if (length == 2 && data[0] != null) {
                return new ArrayAttributes(data);
            }
            return ArrayAttributes.sortAndFilterToAttributes(data);
        } else {
            Object[] newData = new Object[length];
            System.arraycopy(data, 0, newData, 0, length);
            if (length == 2 && newData[0] != null) {
                return new ArrayAttributes(newData);
            }
            return ArrayAttributes.sortAndFilterToAttributes(newData);
        }
    }

    @Override
    public <T> AttributesBuilder put(AttributeKey<Long> key, int value) {
        return put(key, (long) value);
    }

    @Override
    public <T> AttributesBuilder put(AttributeKey<T> key, T value) {
        if (key == null || key.getKey().isEmpty()) {
            return this;
        }
        return set(key, value);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public AttributesBuilder putAll(Attributes attributes) {
        if (attributes == null) {
            return this;
        } else {
            // Attributes must iterate over their entries with matching types for key / value, so this
            // downcast to the raw type is safe.
            attributes.forEach((key, value) -> put((AttributeKey) key, value));
            return this;
        }
    }

    @Override
    public <T> AttributesBuilder remove(AttributeKey<T> key) {
        return key != null && !key.getKey().isEmpty() ? removeIf((entryKey) -> key.getKey().equals(entryKey.getKey()) && key.getType().equals(entryKey.getType())) : this;
    }

    @Override
    public AttributesBuilder removeIf(Predicate<AttributeKey<?>> predicate) {
        if (predicate == null) {
            return this;
        } else {
            for (int i = 0; i < length - 1; i += 2) {
                Object entry = data[i];
                if (entry instanceof AttributeKey && predicate.test((AttributeKey<?>) entry)) {
                    data[i] = null;
                    data[i + 1] = null;
                }
            }
            return this;
        }
    }

    protected Attributes create() {
        if (length == 0) {
            return ArrayAttributes.EMPTY;
        }
        if (length == data.length) {
            return new ArrayAttributes(data);
        }
        Object[] newData = new Object[length];
        System.arraycopy(data, 0, newData, 0, length);
        return new ArrayAttributes(newData);
    }

    protected <T> ArrayAttributesBuilder set(AttributeKey<T> key, T value) {
        if (value == null) {
            return this;
        }
        if (length >= data.length - 1) {
            Object[] newData = new Object[Math.max(data.length, 1) * 2];
            System.arraycopy(data, 0, newData, 0, length);
            data = newData;
        }
        data[length++] = key;
        data[length++] = value;
        return this;
    }
}