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
import io.opentelemetry.api.internal.ImmutableKeyValuePairs;

import javax.annotation.Nullable;
import java.util.Comparator;

public class ArrayAttributes extends ImmutableKeyValuePairs<AttributeKey<?>, Object> implements Attributes {

    // We only compare the key name, not type, when constructing, to allow deduping keys with the
    // same name but different type.
    private static final Comparator<AttributeKey<?>> KEY_COMPARATOR_FOR_CONSTRUCTION = Comparator.comparing(AttributeKey::getKey);

    public static final Attributes EMPTY = new ArrayAttributes(new Object[0]);

    private ArrayAttributes(Object[] data, Comparator<AttributeKey<?>> keyComparator) {
        super(data, keyComparator);
    }

    public ArrayAttributes(Object[] data) {
        super(data);
    }

    @Override
    public AttributesBuilder toBuilder() {
        Object[] data = getData();
        if (data == null || data.length == 0) {
            return new ArrayAttributesBuilder(new Object[0]);
        }
        // copy the array to avoid modifying the original data
        Object[] newData = new Object[data.length];
        System.arraycopy(data, 0, newData, 0, data.length);
        return new ArrayAttributesBuilder(newData);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T get(AttributeKey<T> key) {
        return (T) super.get(key);
    }

    static Attributes sortAndFilterToAttributes(Object... data) {
        // null out any empty keys or keys with null values
        // so they will then be removed by the sortAndFilter method.
        for (int i = 0; i < data.length; i += 2) {
            AttributeKey<?> key = (AttributeKey<?>) data[i];
            if (key != null && key.getKey().isEmpty()) {
                data[i] = null;
            }
        }
        return new ArrayAttributes(data, KEY_COMPARATOR_FOR_CONSTRUCTION);
    }

}
