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

import com.jd.live.agent.core.inject.Injection;
import com.jd.live.agent.core.inject.jbind.*;
import com.jd.live.agent.core.util.option.Option;

public class JInjectionContext implements InjectionContext {
    protected final ConverterSelector converterSelector;
    protected final ArrayFactory arrayFactory;
    protected final Option environment;

    public JInjectionContext(ConverterSelector converterSelector, ArrayFactory arrayFactory, Option environment) {
        this.converterSelector = converterSelector;
        this.arrayFactory = arrayFactory;
        this.environment = environment;
    }

    @Override
    public ArrayBuilder getArrayBuilder(Class<?> componentType) {
        return arrayFactory.getArrayBuilder(componentType);
    }

    @Override
    public Converter getConverter(ConversionType type) {
        return converterSelector.getConverter(type);
    }

    @Override
    public Option getEnvironment() {
        return environment;
    }

    public static class JEmbedInjectionContext implements EmbedInjectionContext {

        private final InjectionContext context;

        private final InjectionSupplier injectionSupplier;

        private String path;

        public JEmbedInjectionContext(InjectionContext context, InjectionSupplier injectionSupplier) {
            this.context = context;
            this.injectionSupplier = injectionSupplier;
        }

        @Override
        public ArrayBuilder getArrayBuilder(Class<?> componentType) {
            return context.getArrayBuilder(componentType);
        }

        @Override
        public Converter getConverter(ConversionType type) {
            return context.getConverter(type);
        }

        @Override
        public Option getEnvironment() {
            return context.getEnvironment();
        }

        @Override
        public Injection build(Class<?> type) {
            return injectionSupplier.build(type, this);
        }

    }
}
