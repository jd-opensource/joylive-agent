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
package com.jd.live.agent.core.classloader;

import com.jd.live.agent.bootstrap.classloader.*;
import com.jd.live.agent.core.util.Close;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The PluginLoaderManager class is responsible for managing class loaders for plugins. It implements
 * the ClassLoaderSupervisor, Resourcer, and Closeable interfaces to provide functionality for
 * creating, retrieving, and managing class loaders, as well as for locating and accessing resources.
 *
 * @since 1.0.0
 */
public class PluginLoaderManager implements ClassLoaderSupervisor, Resourcer, Closeable {

    /**
     * A thread-safe collection of live class loaders, indexed by their name.
     */
    private final Map<String, LiveClassLoader> loaders = new ConcurrentHashMap<>(100);

    /**
     * The factory used to create new class loaders.
     */
    private final ClassLoaderFactory builder;

    /**
     * Constructs a PluginLoaderManager with a specified class loader factory.
     *
     * @param builder The ClassLoaderFactory to use for creating new class loaders.
     */
    public PluginLoaderManager(ClassLoaderFactory builder) {
        this.builder = builder;
    }

    @Override
    public ClassLoader get(String name) {
        return name == null || name.isEmpty() ? null : loaders.get(name);
    }

    @Override
    public ClassLoader remove(String name) {
        return name == null || name.isEmpty() ? null : loaders.remove(name);
    }

    @Override
    public ClassLoader create(String name) {
        return name == null || name.isEmpty() ? null : builder.create(name);
    }

    @Override
    public ClassLoader create(String name, URL[] urls) {
        return name == null || name.isEmpty() ? null : builder.create(name, urls);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false, null);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass(name, resolve, null);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve, CandidatorProvider candidatorProvider) throws ClassNotFoundException {
        if (name != null && !name.isEmpty()) {
            for (ClassLoader classLoader : loaders.values()) {
                try {
                    Class<?> clazz = classLoader.loadClass(name);
                    if (clazz != null) {
                        return clazz;
                    }
                } catch (ClassNotFoundException ignore) {
                }
            }
            if (candidatorProvider != null) {
                ClassLoader candidator = candidatorProvider.getCandidator();
                if (candidator != null)
                    return candidator.loadClass(name);
            }
        }
        throw new ClassNotFoundException("class " + name + " is not found.");
    }

    @Override
    public URL findResource(String path) {
        URL url = null;
        if (path != null) {
            for (ClassLoader classLoader : loaders.values()) {
                url = ((Resourcer) classLoader).findResource(path);
                if (url != null) {
                    break;
                }
            }
        }
        return url;
    }

    @Override
    public Enumeration<URL> findResources(String path) throws IOException {
        List<URL> urls = new LinkedList<>();
        if (path != null) {
            Enumeration<URL> enumeration;
            for (ClassLoader classLoader : loaders.values()) {
                enumeration = ((Resourcer) classLoader).findResources(path);
                if (enumeration != null) {
                    while (enumeration.hasMoreElements()) {
                        urls.add(enumeration.nextElement());
                    }
                }
            }
        }
        return Collections.enumeration(urls);
    }

    @Override
    public URL getResource(String path) throws IOException {
        URL url = null;
        if (path != null) {
            for (ClassLoader classLoader : loaders.values()) {
                url = ((Resourcer) classLoader).getResource(path);
                if (url != null) {
                    break;
                }
            }
        }
        return url;
    }

    @Override
    public Enumeration<URL> getResources(String path) throws IOException {
        List<URL> urls = new LinkedList<>();
        if (path != null) {
            Enumeration<URL> enumeration;
            for (ClassLoader classLoader : loaders.values()) {
                enumeration = ((Resourcer) classLoader).getResources(path);
                if (enumeration != null) {
                    while (enumeration.hasMoreElements()) {
                        urls.add(enumeration.nextElement());
                    }
                }
            }
        }
        return Collections.enumeration(urls);
    }

    @Override
    public ResourcerType getType() {
        return ResourcerType.PLUGIN;
    }

    @Override
    public void close() {
        loaders.values().forEach(o -> Close.instance().close(o));
        loaders.clear();
    }
}
