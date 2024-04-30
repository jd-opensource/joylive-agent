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
package com.jd.live.agent.core.extension.jplug;

import com.jd.live.agent.core.extension.ExtensibleDesc;
import com.jd.live.agent.core.extension.ExtensibleLoader;
import com.jd.live.agent.core.extension.ExtensionManager;

public class JExtensibleLoader<T> implements ExtensibleLoader<T> {
    private final ExtensionManager extensionManager;
    private final Class<T> extensible;
    private final ClassLoader classLoader;

    public JExtensibleLoader(ExtensionManager extensionManager, Class<T> extensible, ClassLoader classLoader) {
        this.extensionManager = extensionManager;
        this.extensible = extensible;
        this.classLoader = classLoader;
    }

    public T getExtension(String name) {
        return extensionManager.getExtension(extensible, name);
    }

    public T getOrLoadExtension() {
        return extensionManager.getOrLoadExtension(extensible);
    }

    public ExtensibleDesc<T> getOrLoadExtensible() {
        return extensionManager.getOrLoadExtensible(extensible, classLoader);
    }

    public ExtensibleDesc<T> loadExtensible() {
        return extensionManager.loadExtensible(extensible, classLoader);
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public Class<T> getExtensible() {
        return extensible;
    }

    @Override
    public void close() {
        extensionManager.remove(extensible);
    }
}
