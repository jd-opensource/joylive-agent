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
package com.jd.live.agent.core.inject.jbind.converter;

import com.jd.live.agent.core.inject.jbind.ConversionType;
import com.jd.live.agent.core.inject.jbind.Converter;
import com.jd.live.agent.core.inject.jbind.Converter.FundamentalConverter;
import com.jd.live.agent.core.inject.jbind.ConverterSupplier;
import com.jd.live.agent.core.inject.jbind.TypeInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FundamentalSupplier implements ConverterSupplier {
    private final Map<Class<?>, Map<Class<?>, FundamentalConverter>> fundamentals = new HashMap<>(100);

    public FundamentalSupplier(List<FundamentalConverter> fundamentalConverters) {
        if (fundamentalConverters != null) {
            for (FundamentalConverter conv : fundamentalConverters) {
                fundamentals.computeIfAbsent(conv.getSourceType(), t -> new HashMap<>(100)).put(conv.getTargetType(), conv);
            }
        }
    }

    @Override
    public Converter getConverter(ConversionType type) {
        TypeInfo sourceType = type.getSourceType();
        Class<?> sourceInboxType = sourceType.getInboxType();
        Map<Class<?>, FundamentalConverter> sourceConverters = fundamentals.get(sourceInboxType);
        if (sourceConverters == null) {
            // lookup superclass
            for (Map.Entry<Class<?>, Map<Class<?>, FundamentalConverter>> entry : fundamentals.entrySet()) {
                if (entry.getKey().isAssignableFrom(sourceInboxType)) {
                    sourceConverters = entry.getValue();
                    break;
                }
            }
        }
        return sourceConverters == null ? null : sourceConverters.get(type.getTargetType().getInboxType());
    }
}


