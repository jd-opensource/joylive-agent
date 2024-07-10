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
import com.jd.live.agent.core.extension.ExtensionDesc;
import com.jd.live.agent.core.extension.Name;

import java.util.*;
import java.util.function.Predicate;

/**
 * This class represents a container for extensions, providing mechanisms to access and manage
 * multiple extensions of a specific type.
 *
 * @param <T> The type of the extensions contained within this extensible container.
 */
public class JExtensible<T> implements ExtensibleDesc<T> {

    /**
     * The name representing the type of the extensions.
     */
    private final Name<T> name;

    /**
     * The list of extension descriptors.
     */
    private final List<ExtensionDesc<T>> extensions;

    /**
     * A map to hold providers for the extensions, allowing retrieval by a unique key.
     */
    private final Map<String, ExtensionDesc<T>> providers = new HashMap<>();

    /**
     * A map to hold the best (typically highest priority) extension descriptor for each name.
     */
    private final Map<String, ExtensionDesc<T>> nameBests = new LinkedHashMap<>();

    /**
     * A map to hold all extension descriptors associated with each name.
     */
    private final Map<String, List<ExtensionDesc<T>>> nameAll = new HashMap<>();

    /**
     * A flag indicating whether all contained extensions are singletons.
     */
    private boolean singleton = true;

    /**
     * A volatile list to hold the actual plug instances (if they are singletons).
     */
    private volatile List<T> plugs;

    private volatile Map<String, T> namePlugs;

    /**
     * Constructs a JExtensible container with a given name and a list of extension descriptors.
     *
     * @param name       The name representing the type of the extensions.
     * @param extensions The list of extension descriptors to be contained.
     */
    public JExtensible(Name<T> name, List<ExtensionDesc<T>> extensions) {
        this.name = name;
        this.extensions = Collections.unmodifiableList(extensions);
        for (ExtensionDesc<T> extension : extensions) {
            String naming = extension.getName().getName();
            String provider = extension.getProvider();
            if (provider != null && !provider.isEmpty()) {
                providers.putIfAbsent(naming + "@" + provider, extension);
            }
            nameBests.putIfAbsent(naming, extension);
            nameAll.computeIfAbsent(naming, s -> new ArrayList<>()).add(extension);
            if (!extension.isSingleton()) {
                singleton = false;
            }
        }
    }

    @Override
    public Name<T> getName() {
        return name;
    }

    @Override
    public T getExtension() {
        return extensions.isEmpty() ? null : extensions.get(0).getTarget();
    }

    @Override
    public T getExtension(String name) {
        if (name == null || name.isEmpty())
            return null;
        ExtensionDesc<T> result = nameBests.get(name);
        if (result == null) {
            int pos = name.indexOf('@');
            if (pos > 0) {
                result = providers.get(name);
                if (result == null) {
                    result = nameBests.get(name.substring(0, pos));
                }
            }
        }
        return result == null ? null : result.getTarget();
    }

    @Override
    public T getExtension(String... name) {
        T result;
        for (String n : name) {
            result = getExtension(n);
            if (result != null)
                return result;
        }
        return null;
    }

    @Override
    public int getSize() {
        return extensions.size();
    }

    @Override
    public List<T> getExtensions() {
        return getOrCreateExtensions();
    }

    @Override
    public Map<String, T> getExtensionMap() {
        Map<String, T> result;
        if (singleton) {
            result = namePlugs;
            if (result == null) {
                synchronized (this) {
                    if (namePlugs == null) {
                        result = createExtensionMap();
                        namePlugs = result;
                    } else {
                        result = namePlugs;
                    }
                }
            }
        } else {
            result = createExtensionMap();
        }
        return result;
    }

    private Map<String, T> createExtensionMap() {
        Map<String, T> result = new LinkedHashMap<>(nameBests.size());
        for (Map.Entry<String, ExtensionDesc<T>> entry : nameBests.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getTarget());
        }
        return Collections.unmodifiableMap(result);
    }

    private List<T> getOrCreateExtensions() {
        List<T> targets;
        if (singleton) {
            targets = plugs;
            if (targets == null) {
                synchronized (this) {
                    if (plugs == null) {
                        targets = createExtensions();
                        plugs = targets;
                    } else {
                        targets = plugs;
                    }
                }
            }
        } else {
            targets = createExtensions();
        }
        return targets;
    }

    private List<T> createExtensions() {
        List<T> result = new LinkedList<>();
        for (ExtensionDesc<T> desc : extensions) {
            result.add(desc.getTarget());
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Iterable<T> getExtensions(Predicate<T> predicate, boolean reverse) {
        List<T> targets = getOrCreateExtensions();
        if (targets.isEmpty() || predicate == null && !reverse)
            return targets;
        else {
            LinkedList<T> result = new LinkedList<>();
            for (T target : targets) {
                if (predicate == null || predicate.test(target)) {
                    if (!reverse)
                        result.add(target);
                    else
                        result.addFirst(target);
                }
            }
            return Collections.unmodifiableList(result);
        }
    }

    @Override
    public List<String> getExtensionNames() {
        return new ArrayList<>(nameBests.keySet());
    }

    @Override
    public List<String> contains(List<String> names) {
        List<String> result = new ArrayList<>();
        if (names != null) {
            for (String n : names) {
                if (nameBests.containsKey(n)) {
                    result.add(n);
                }
            }
        }
        return result;
    }

    @Override
    public ExtensionDesc<T> getExtensionDesc(String name) {
        return nameBests.get(name);
    }

    @Override
    public Iterable<ExtensionDesc<T>> getExtensionDescs() {
        return extensions;
    }

    @Override
    public Iterable<ExtensionDesc<T>> getExtensionDescs(String name) {
        List<ExtensionDesc<T>> result = name == null || name.isEmpty() ? new ArrayList<>(0) : nameAll.get(name);
        return Collections.unmodifiableList(result);
    }
}
