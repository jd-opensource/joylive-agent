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

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.Injection;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Configurable;
import com.jd.live.agent.core.inject.jbind.AbstractAnnotationSupplier;
import com.jd.live.agent.core.inject.jbind.InjectType;
import com.jd.live.agent.core.inject.jbind.InjectType.InjectField;
import com.jd.live.agent.core.inject.jbind.InjectionContext;
import com.jd.live.agent.core.inject.jbind.InjectionSupplier;
import com.jd.live.agent.core.util.cache.CacheObject;
import com.jd.live.agent.core.util.type.FieldDesc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * scan field with @config annotation and then build an injection.
 *
 * @since 1.0.0
 */
@Extension(value = InjectionSupplier.CONFIG_ANNOTATION_SUPPLIER)
public class JConfigAnnotationSupplier extends AbstractAnnotationSupplier {
    private final Map<Class<?>, CacheObject<Injection>> noneEmbedTypes = new ConcurrentHashMap<>(1000);
    private final Map<Class<?>, CacheObject<Injection>> embedTypes = new ConcurrentHashMap<>(1000);

    @Override
    protected Map<Class<?>, CacheObject<Injection>> getCache(Class<?> type, InjectionContext context) {
        return context.isEmbed() ? embedTypes : noneEmbedTypes;
    }

    @Override
    protected InjectType createType(Class<?> type, InjectionContext context) {
        return new InjectType(type, context.isEmbed() ? null : type.getAnnotation(Configurable.class));
    }

    @Override
    protected InjectField createField(InjectType injectType, FieldDesc fieldDesc, InjectionContext context) {
        boolean embed = context.isEmbed();
        Config config = fieldDesc.getAnnotation(Config.class);
        Configurable configurable = (Configurable) injectType.getAnnotation();
        if (embed || config != null || (configurable != null && configurable.auto())) {
            String key = config == null ? null : config.value();
            if (key == null || key.isEmpty())
                key = fieldDesc.getName();
            return new InjectField(fieldDesc.getField(), key, fieldDesc, config, fieldDesc.getGeneric(),
                    new JConfigSourcer(key, context.getEnvironment()));
        }
        return null;
    }

    @Override
    protected Injection createInjection(InjectType injectType, InjectionContext context) {
        return new JConfigAnnotationInjection(injectType, context, this);
    }

}
