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
package com.jd.live.agent.core.inject.jbind.converter.supplier;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.jbind.ConverterSupplier;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Extension(value = "ValueOfSupplier", order = ConverterSupplier.VALUE_OF_SUPPLIER_ORDER)
public class ValueOfSupplier extends FactoryMethodSupplier {

    public static final String VALUE_OF = "valueOf";
    protected static ConcurrentMap<Class<?>, ConcurrentMap<Class<?>, Optional<Method>>> methods =
            new ConcurrentHashMap<>();

    public ValueOfSupplier() {
        super(VALUE_OF);
    }

    @Override
    protected ConcurrentMap<Class<?>, ConcurrentMap<Class<?>, Optional<Method>>> getCache() {
        return methods;
    }
}
