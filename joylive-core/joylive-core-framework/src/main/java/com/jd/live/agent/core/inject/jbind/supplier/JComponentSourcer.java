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
package com.jd.live.agent.core.inject.jbind.supplier;

import com.jd.live.agent.core.inject.InjectSource;
import com.jd.live.agent.core.inject.jbind.Sourcer;

import java.util.Map;

/**
 * ComponentSourcer
 *
 * @since 1.0.0
 */
public class JComponentSourcer implements Sourcer {

    protected final String name;

    protected final Class<?> type;

    public JComponentSourcer(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public Object getSource(Object context) {
        return getObject(context, name, type);
    }

    @SuppressWarnings("unchecked")
    protected <T> T getObject(Object context, String name, Class<T> type) {
        Map<?, ?> components = context instanceof InjectSource ?
                ((InjectSource) context).getComponents() : (
                context instanceof Map ? (Map<?, ?>) context : null);
        Object result = components == null ? null : components.get(name);
        if (result != null && !type.isAssignableFrom(result.getClass()))
            result = null;
        if (result == null && components != null) {
            for (Map.Entry<?, ?> entry : components.entrySet()) {
                if (type.isAssignableFrom(entry.getValue().getClass())) {
                    result = entry.getValue();
                    break;
                }
            }
        }
        return (T) result;
    }
}
