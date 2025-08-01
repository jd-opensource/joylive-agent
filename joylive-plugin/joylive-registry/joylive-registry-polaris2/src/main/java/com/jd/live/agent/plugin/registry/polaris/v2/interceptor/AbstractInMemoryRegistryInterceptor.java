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
package com.jd.live.agent.plugin.registry.polaris.v2.interceptor;

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.tencent.polaris.plugins.registry.memory.InMemoryRegistry;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;

/**
 * AbstractInMemoryRegistryInterceptor
 */
public class AbstractInMemoryRegistryInterceptor extends InterceptorAdaptor {

    protected static class Accessor {

        protected static final FieldAccessor persistExecutor = getAccessor(InMemoryRegistry.class, "persistExecutor");

    }

}
