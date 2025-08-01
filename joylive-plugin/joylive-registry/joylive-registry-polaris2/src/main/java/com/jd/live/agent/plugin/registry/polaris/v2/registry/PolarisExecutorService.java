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
package com.jd.live.agent.plugin.registry.polaris.v2.registry;

import com.jd.live.agent.core.util.ExecutorServiceAdaptor;
import com.jd.live.agent.governance.registry.RegistryService;

import java.util.concurrent.ExecutorService;

public class PolarisExecutorService extends ExecutorServiceAdaptor {

    private final RegistryService registryService;

    public PolarisExecutorService(ExecutorService delegate, RegistryService registryService) {
        super(delegate);
        this.registryService = registryService;
    }

    public RegistryService getRegistryService() {
        return registryService;
    }

}
