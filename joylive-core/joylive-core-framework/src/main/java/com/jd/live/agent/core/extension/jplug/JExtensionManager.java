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

import com.jd.live.agent.core.extension.*;
import com.jd.live.agent.core.extension.ExtensionEvent.EventType;
import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.core.extension.condition.ConditionMatcher;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * JExtensionManager
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class JExtensionManager implements ExtensionManager {

    private final ConditionMatcher matcher;

    private final Map<Class<?>, ExtensibleDesc<?>> extensibles = new ConcurrentHashMap<>();

    private final Map<String, ExtensibleDesc<?>> names = new ConcurrentHashMap<>();

    private final List<ExtensionListener> listeners = new CopyOnWriteArrayList<>();

    private final ExtensionListener listener;

    public JExtensionManager(ConditionMatcher matcher) {
        this.matcher = matcher;
        listener = (event) -> {
            listeners.forEach(l -> l.onEvent(event));
            if (event.getType() == EventType.CREATED && event.getInstance() instanceof ExtensionInitializer) {
                ((ExtensionInitializer) event.getInstance()).initialize();
            }
        };
    }

    public JExtensionManager() {
        this(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getExtension(String type, String name) {
        ExtensibleDesc<?> extensibleDesc = type == null || type.isEmpty() ? null : names.get(type);
        return extensibleDesc == null ? null : (T) extensibleDesc.getExtension(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getExtension(Class<T> extensible, String name) {
        ExtensibleDesc<?> extensibleDesc = extensible == null ? null : extensibles.get(extensible);
        return extensibleDesc == null ? null : (T) extensibleDesc.getExtension(name);
    }

    @Override
    public <T> T getOrLoadExtension(Class<T> extensible) {
        return getOrLoadExtension(extensible, (ClassLoader) null);
    }

    @Override
    public <T> T getOrLoadExtension(Class<T> extensible, ClassLoader classLoader) {
        ExtensibleDesc<T> extensibleDesc = extensible == null ? null : getOrLoadExtensible(extensible, classLoader);
        return extensibleDesc == null ? null : extensibleDesc.getExtension();
    }

    @Override
    public <T> T getOrLoadExtension(Class<T> extensible, String name) {
        return getOrLoadExtension(extensible, name, null);
    }

    @Override
    public <T> T getOrLoadExtension(Class<T> extensible, String name, ClassLoader classLoader) {
        if (extensible == null) {
            return null;
        }
        T result = getExtension(extensible, name);
        if (result == null) {
            ExtensibleDesc<T> extensibleDesc = loadExtensible(extensible, classLoader);
            if (extensibleDesc != null) {
                result = extensibleDesc.getExtension(name);
            }
        }
        return result;
    }

    @Override
    public <T> ExtensibleDesc<T> getOrLoadExtensible(Class<T> extensible) {
        return getOrLoadExtensible(extensible, (ClassLoader) null);
    }

    @Override
    public <T> ExtensibleDesc<T> getOrLoadExtensible(Class<T> extensible, ClassLoader classLoader) {
        return getOrLoadExtensible(extensible, new JExtensionLoader(classLoader, matcher, listener));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ExtensibleDesc<T> getOrLoadExtensible(Class<T> extensible, ExtensionLoader extensionLoader) {
        if (extensible == null) {
            return null;
        }
        ExtensibleDesc<?> result = extensibles.get(extensible);
        if (result == null) {
            result = loadExtensible(extensible, extensionLoader);
            ExtensibleDesc<?> exists = extensibles.putIfAbsent(extensible, result);
            if (exists == null) {
                names.put(result.getName().getName(), result);
            } else {
                result = exists;
            }
        }
        return (ExtensibleDesc<T>) result;
    }

    @Override
    public <T> ExtensibleDesc<T> loadExtensible(Class<T> extensible) {
        return loadExtensible(extensible, (ClassLoader) null);
    }

    @Override
    public <T> ExtensibleDesc<T> loadExtensible(Class<T> extensible, ClassLoader classLoader) {
        return loadExtensible(extensible, new JExtensionLoader(classLoader, matcher, listener));
    }

    @Override
    public <T> ExtensibleDesc<T> loadExtensible(Class<T> extensible, ExtensionLoader extensionLoader) {
        if (extensible == null) {
            return null;
        }
        List<ExtensionDesc<T>> extensionDescList = extensionLoader.load(extensible);
        Extensible annotation = extensible.getAnnotation(Extensible.class);
        Name<T> name = new Name<>(extensible, annotation != null && !annotation.value().isEmpty() ? annotation.value() : extensible.getName());
        // Sort
        Comparator<ExtensionDesc<?>> c = ExtensionDesc.AscendingComparator.INSTANCE;
        extensionDescList.sort(c);
        // No need to cache objects in the loading method
        return new JExtensible<>(name, extensionDescList);
    }

    @Override
    public <T> ExtensibleLoader<T> build(Class<T> extensible, ClassLoader classLoader) {
        return extensible == null ? null : new JExtensibleLoader<>(this, extensible, classLoader);
    }

    @Override
    public void remove(ClassLoader classLoader) {
        if (classLoader != null) {
            for (Map.Entry<Class<?>, ExtensibleDesc<?>> entry : extensibles.entrySet()) {
                if (entry.getKey().getClassLoader() == classLoader) {
                    extensibles.remove(entry.getKey());
                    names.remove(entry.getValue().getName().getName());
                }
            }
        }
    }

    @Override
    public void remove(Class<?> extensible) {
        if (extensible != null) {
            ExtensibleDesc<?> desc = extensibles.remove(extensible);
            if (desc != null) {
                names.remove(desc.getName().getName());
            }
        }
    }

    @Override
    public void addListener(ExtensionListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(ExtensionListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
}
