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

import com.jd.live.agent.core.exception.ConvertException;
import com.jd.live.agent.core.exception.InjectException;
import com.jd.live.agent.core.inject.InjectSource;
import com.jd.live.agent.core.inject.Injection;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Configurable;
import com.jd.live.agent.core.inject.jbind.*;
import com.jd.live.agent.core.inject.jbind.InjectType.InjectField;
import com.jd.live.agent.core.inject.jbind.supplier.JInjectionContext.JEmbedInjectionContext;
import com.jd.live.agent.core.util.option.CascadeOption;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.core.util.type.ClassDesc;
import com.jd.live.agent.core.util.type.ClassUtils;

import java.lang.reflect.Constructor;
import java.util.Map;

import static com.jd.live.agent.core.util.type.ClassUtils.*;

/**
 * handler of @config
 */
public class JConfigAnnotationInjection extends AbstractInjection {
    private final JEmbedInjectionContext context;
    private final String configPrefix;

    public JConfigAnnotationInjection(InjectType injectType, InjectionContext context, InjectionSupplier injectionSupplier) {
        super(injectType);
        Configurable configurable = (Configurable) injectType.getAnnotation();
        this.configPrefix = configurable == null ? "" : configurable.prefix();
        this.context = new JEmbedInjectionContext(context, injectionSupplier);
    }

    @Override
    public void inject(Object source, Object target) {
        JSource typeSrc = build(source);
        Object value;
        JSource fieldSrc;
        for (InjectField field : injectType.getFields()) {
            fieldSrc = typeSrc.build(field.getKey());
            value = field.getSource(fieldSrc);
            if (value != null) {
                value = convert(field, value, fieldSrc.getPath());
                fieldSrc.setUpdated(true);
            } else if (!fieldSrc.cascade()) {
                value = createAndInject(fieldSrc, field);
            }
            if (value == null) {
                Config configAnnotation = (Config) field.getAnnotation();
                if (configAnnotation != null && !configAnnotation.nullable()) {
                    Object defaultValue = field.get(target);
                    if (defaultValue == null) {
                        throw new InjectException("config is not allowed empty. path=" + fieldSrc.getPath());
                    }
                }
            } else if (fieldSrc.isUpdated()) {
                if (!typeSrc.isUpdated()) {
                    typeSrc.setUpdated(true);
                    if (source instanceof JSource) {
                        ((JSource) source).setUpdated(true);
                    }
                }
                field.set(target, value);
            }
        }
    }

    private Object convert(InjectField field, Object value, String fieldPath) {
        Class<?> fieldCls = field.getType();
        Class<?> sourceCls = value.getClass();
        Class<?> sourceInboxCls = inbox(sourceCls);
        Class<?> targetInboxCls = inbox(fieldCls);
        ConversionType conversionType = new ConversionType(
                new TypeInfo(value.getClass(), sourceInboxCls),
                new TypeInfo(fieldCls, targetInboxCls, field.getGenericType()));
        Converter converter = context.getConverter(conversionType);
        Conversion conversion = new Conversion(conversionType, value, context);
        conversion.setPath(fieldPath);
        try {
            return converter.convert(conversion);
        } catch (ConvertException e) {
            throw e;
        } catch (Exception e) {
            throw new ConvertException("failed to convert between " + conversion.getSourceType().getRawType() +
                    " and " + conversion.getTargetType().getRawType() +
                    ". path=" + conversion.getPath(), e);
        }
    }

    private Object createAndInject(JSource source, InjectField field) {
        Class<?> fieldCls = field.getType();
        if (isEntity(fieldCls)) {
            ClassDesc classDesc = describe(fieldCls);
            if (!classDesc.getFieldList().isEmpty()) {
                Object obj = null;
                Constructor<?> constructor = classDesc.getConstructorList().getDefaultConstructor();
                if (constructor != null) {
                    try {
                        obj = constructor.newInstance();
                    } catch (Throwable e) {
                        throw new InjectException("instantiate class error " + fieldCls + ", caused by " + e.getMessage());
                    }
                }
                Injection injection = context.build(fieldCls);
                if (injection == null) {
                    throw new InjectException("The injection is not found for " + field.getField().toString());
                }
                injection.inject(source, obj);
                return obj;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private JSource build(Object source) {
        JSource src = source instanceof JSource ? (JSource) source : null;
        String path = src != null ? src.getPath() : configPrefix;
        Object root = src != null ? src.getRoot() : null;
        Object parent = src != null ? src.getParent() : null;
        Object current = src != null ? src.getCurrent() : source;
        Option option = null;
        if (current instanceof InjectSource) {
            option = ((InjectSource) current).getOption();
        } else if (current instanceof Option) {
            option = (Option) current;
        } else if (current instanceof Map) {
            option = new CascadeOption((Map<String, Object>) current);
        }
        if (option != null) {
            if (path == null || path.isEmpty()) {
                current = option;
            } else {
                current = option.getObject(path);
                parent = option;
            }
        }
        if (root == null) {
            root = option != null ? option : current;
        }
        return new JSource(current, parent, root, path);
    }

}
