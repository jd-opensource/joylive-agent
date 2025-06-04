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
package com.jd.live.agent.governance.registry;

public class RegistryRunnable implements RegistryCallable<Void> {

    private final RegistryService registry;

    private final Runnable runnable;

    public RegistryRunnable(RegistryService registry, Runnable runnable) {
        this.registry = registry;
        this.runnable = runnable;
    }

    @Override
    public Void call() throws Exception {
        runnable.run();
        return null;
    }

    @Override
    public RegistryService getRegistry() {
        return registry;
    }
}
