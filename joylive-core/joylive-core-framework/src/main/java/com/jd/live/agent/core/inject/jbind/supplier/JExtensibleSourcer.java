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

import com.jd.live.agent.bootstrap.classloader.Resourcer;
import com.jd.live.agent.bootstrap.classloader.ResourcerType;
import com.jd.live.agent.core.extension.ExtensibleDesc;
import com.jd.live.agent.core.extension.ExtensionManager;

/**
 * JExtensibleSourcer
 *
 * @since 1.0.0
 */
public class JExtensibleSourcer extends JComponentSourcer {
    protected final Class<?> owner;
    protected final ResourcerType resourcerType;

    public JExtensibleSourcer(String name, Class<?> type, Class<?> owner, ResourcerType resourcerType) {
        super(name, type);
        this.owner = owner;
        this.resourcerType = resourcerType;
    }

    @Override
    public Object getSource(Object context) {
        return getExtensible(context);
    }

    protected ExtensibleDesc<?> getExtensible(Object context) {
        ExtensionManager extensionManager = getObject(context, ExtensionManager.COMPONENT_EXTENSION_MANAGER, ExtensionManager.class);
        // class is loaded by plugin classloader
        ClassLoader ownerLoader = owner.getClassLoader();
        ResourcerType rt = resourcerType == null && ownerLoader instanceof Resourcer ? ((Resourcer) ownerLoader).getType() : resourcerType;
        rt = rt == null ? ResourcerType.CORE_IMPL : rt;
        switch (rt) {
            case CORE:
                ClassLoader coreLoader = getObject(context, Resourcer.COMPONENT_CLASSLOADER_CORE, ClassLoader.class);
                return extensionManager.getOrLoadExtensible(type, coreLoader == null ? type.getClassLoader() : coreLoader);
            case CORE_IMPL:
                ClassLoader coreImplLoader = getObject(context, Resourcer.COMPONENT_CLASSLOADER_CORE_IMPL, ClassLoader.class);
                return extensionManager.getOrLoadExtensible(type, coreImplLoader == null ? type.getClassLoader() : coreImplLoader);
            case PLUGIN:
                return extensionManager.loadExtensible(type, ownerLoader);
            default:
                return null;
        }

    }
}
