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

import com.jd.live.agent.core.exception.InjectException;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.jbind.AbstractInjection;
import com.jd.live.agent.core.inject.jbind.InjectType;
import com.jd.live.agent.core.inject.jbind.InjectType.InjectField;

/**
 * Handler of @Inject
 */
public class JInjectAnnotationInjection extends AbstractInjection {

    public JInjectAnnotationInjection(InjectType injectType) {
        super(injectType);
    }

    @Override
    public void inject(Object source, Object target) {
        if (!injectType.isEmpty()) {
            for (InjectField field : injectType.getFields()) {
                Object value = field.getSource(source);
                if (value == null) {
                    Inject inject = (Inject) field.getAnnotation();
                    if (!inject.nullable()) {
                        throw new InjectException("target is not allowed null. key: " + field.getKey() + ", field " + injectType.getType().getName() + "." + field.getName());
                    }
                } else {
                    field.set(target, value);
                }
            }
        }
    }
}
