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

import com.jd.live.agent.core.extension.ExtensionManager;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.Injection;
import com.jd.live.agent.core.inject.InjectorFactory;
import com.jd.live.agent.core.inject.jbind.*;
import com.jd.live.agent.core.inject.jbind.converter.BestSelector;
import com.jd.live.agent.core.inject.jbind.supplier.JInjectionContext.JEmbedInjectionContext;
import com.jd.live.agent.core.util.option.MapOption;
import com.jd.live.agent.core.util.option.Option;

import java.util.List;

@Extension("JInjectorFactory")
public class JInjectorFactory implements InjectorFactory {

    @Override
    public Injection create(ExtensionManager extensionManager) {
        return create(extensionManager, null, null, false);
    }

    @Override
    public Injection create(ExtensionManager extensionManager, ClassLoader classLoader) {
        return create(extensionManager, null, classLoader, false);
    }

    @Override
    public Injection create(ExtensionManager extensionManager, Option environment) {
        return create(extensionManager, environment, null, false);
    }

    @Override
    public Injection create(ExtensionManager extensionManager, Option environment, ClassLoader classLoader) {
        return create(extensionManager, environment, classLoader, false);
    }

    @Override
    public Injection create(ExtensionManager extensionManager, Option environment, ClassLoader classLoader, boolean embed) {
        if (extensionManager == null)
            return null;
        environment = environment == null ? MapOption.environment() : environment;
        classLoader = classLoader == null ? JInjectorFactory.class.getClassLoader() : classLoader;
        List<ConverterSupplier> converterSuppliers = extensionManager.getOrLoadExtensible(ConverterSupplier.class, classLoader).getExtensions();
        List<Converter.FundamentalConverter> fundamentalConverters = extensionManager.getOrLoadExtensible(Converter.FundamentalConverter.class, classLoader).getExtensions();
        List<ArrayBuilder> arrayBuilders = extensionManager.getOrLoadExtensible(ArrayBuilder.class, classLoader).getExtensions();
        BestSelector bestSelector = new BestSelector(converterSuppliers, fundamentalConverters, arrayBuilders);
        List<InjectionSupplier> suppliers = extensionManager.getOrLoadExtensible(InjectionSupplier.class, classLoader).getExtensions();
        InjectionContext context;
        if (!embed) {
            context = new JInjectionContext(bestSelector, bestSelector, environment);
        } else {
            InjectionSupplier injectionSupplier = extensionManager.getExtension(InjectionSupplier.class, InjectionSupplier.CONFIG_ANNOTATION_SUPPLIER);
            context = new JEmbedInjectionContext(new JInjectionContext(bestSelector, bestSelector, environment), injectionSupplier);
        }
        return new Injectors(suppliers, context);
    }

}
