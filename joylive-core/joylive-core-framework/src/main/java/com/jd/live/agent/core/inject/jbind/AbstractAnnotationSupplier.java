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

import com.jd.live.agent.core.inject.Injection;
import com.jd.live.agent.core.util.cache.CacheObject;
import com.jd.live.agent.core.util.type.FieldDesc;
import com.jd.live.agent.core.util.type.FieldList;
import com.jd.live.agent.core.util.type.TypeScanner;

import java.lang.reflect.Modifier;
import java.util.Map;

import static com.jd.live.agent.core.util.type.ClassUtils.describe;

/**
 * AbstractAnnotationSupplier
 *
 * @since 1.0.0
 */
public abstract class AbstractAnnotationSupplier implements InjectionSupplier {

    @Override
    public Injection build(Class<?> type, InjectionContext context) {
        if (!TypeScanner.UNTIL_OBJECT.test(type)) {
            return null;
        }
        Map<Class<?>, CacheObject<Injection>> injections = getCache(type, context);
        return injections.computeIfAbsent(type, t -> {
            InjectType injectType = createType(type, context);
            FieldList fieldList = describe(type).getFieldList();
            for (FieldDesc fieldDesc : fieldList.getFields()) {
                int modifiers = fieldDesc.getModifiers();
                if (!Modifier.isFinal(modifiers) && !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)) {
                    InjectType.InjectField field = createField(injectType, fieldDesc, context);
                    if (field != null) {
                        injectType.add(field);
                    }
                }
            }
            Injection injection = injectType == null || injectType.isEmpty() ? null : createInjection(injectType, context);
            return CacheObject.of(injection);
        }).get();
    }

    protected abstract Map<Class<?>, CacheObject<Injection>> getCache(Class<?> type, InjectionContext context);

    protected InjectType createType(Class<?> type, InjectionContext context) {
        return new InjectType(type);
    }

    protected abstract InjectType.InjectField createField(InjectType injectType, FieldDesc fieldDesc, InjectionContext context);

    protected abstract Injection createInjection(InjectType injectType, InjectionContext context);

}
