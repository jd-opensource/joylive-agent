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

import com.jd.live.agent.core.exception.InjectException;
import com.jd.live.agent.core.inject.Injection;

import java.util.ArrayList;
import java.util.List;

/**
 * Injectors is a class responsible for managing the injection of dependencies into objects.
 * It maintains a list of suppliers capable of providing instances of objects, and it uses
 * these suppliers to build and inject dependencies into target objects.
 *
 * @since 1.0.0
 */
public class Injectors implements Injection {

    /**
     * A list of suppliers that are used to provide instances of objects for injection.
     */
    private final List<InjectionSupplier> suppliers;

    /**
     * The context in which the injections are performed.
     */
    private final InjectionContext context;

    /**
     * Constructs a new Injectors instance with the specified suppliers and context.
     *
     * @param suppliers a list of InjectionSupplier instances
     * @param context the InjectionContext for the injections
     */
    public Injectors(List<InjectionSupplier> suppliers, InjectionContext context) {
        this.suppliers = suppliers;
        this.context = context;
    }

    @Override
    public void inject(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        try {
            List<Injection> injections = build(target.getClass());
            for (Injection injection : injections) {
                injection.inject(source, target);
            }
        } catch (InjectException e) {
            throw e;
        } catch (Throwable e) {
            throw new InjectException(e.getMessage(), e);
        }
    }

    /**
     * Builds a list of Injection instances for the specified class type using the registered suppliers.
     * Each supplier is invoked to build an Injection instance, and non-null results are added to the list.
     *
     * @param type the Class object representing the type of objects for which to build Injection instances
     * @return a list of Injection instances that can be used to inject dependencies
     */
    protected List<Injection> build(Class<?> type) {
        List<Injection> injections = new ArrayList<>(10);
        Injection injection;
        for (InjectionSupplier supplier : suppliers) {
            injection = supplier.build(type, context);
            if (injection != null) {
                injections.add(injection);
            }
        }
        return injections;
    }

}

