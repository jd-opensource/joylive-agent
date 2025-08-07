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
import com.jd.live.agent.core.config.ClassLoaderConfig;
import com.jd.live.agent.core.util.Close;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

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

    private final ClassLoaderConfig config;

    private final LiveClassLoader parent;

    /**
     * The factory used to create new class loaders.
     */
    private final ClassLoaderFactory builder;

    public PluginLoaderManager(ClassLoaderConfig config, LiveClassLoader parent, ClassLoaderFactory builder) {
        this.config = config;
        this.parent = parent;
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
    public LiveClassLoader create(String name) {
        LiveClassLoader loader = name == null || name.isEmpty() ? null : builder.create(name);
        if (loader != null) {
            loaders.put(name, loader);
        }
        return loader;
    }

    @Override
    public LiveClassLoader create(String name, URL[] urls) {
        LiveClassLoader loader = name == null || name.isEmpty() ? null : builder.create(name, urls);
        if (loader != null) {
            loaders.put(name, loader);
        }
        return loader;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass(name, resolve, null);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve, Predicate<ClassLoader> predicate) throws ClassNotFoundException {
        // this method is called by spring class loader, so only load classes with "com.jd.live.agent." prefix.
        ResourceConfig rc = config.getPluginResource();
        if (rc.isSelf(name)) {
            return findClass(name, resolve);
        } else if (rc.isParent(name)) {
            return parent.loadClass(name, resolve, predicate);
        }
        throw new ClassNotFoundException("class " + name + " is not found.");
    }

    @Override
    public Class<?> findClass(String name, boolean resolve) throws ClassNotFoundException {
        return LiveClassLoader.getCandidateFeature().disableAndRun(() -> {
            for (Map.Entry<String, LiveClassLoader> entry : loaders.entrySet()) {
                try {
                    return entry.getValue().findClass(name, resolve);
                } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                    // ignore
                }
            }
            throw new ClassNotFoundException("class " + name + " is not found.");
        });
    }

    @Override
    public URL findResource(String path) {
        // This method is called back when the Spring class loader fails to load, only considering classes.
        return findResource(path, false);
    }

    @Override
    public Enumeration<URL> findResources(String path) throws IOException {
        // This method is called back when the Spring class loader fails to load, only considering classes.
        URL url = findResource(path, false);
        return url == null ? null : Collections.enumeration(Collections.singleton(url));
    }

    @Override
    public URL getResource(String path) throws IOException {
        // This method is called back when the Spring class loader fails to load, only considering classes.
        return findResource(path, true);
    }

    @Override
    public Enumeration<URL> getResources(String path) throws IOException {
        // This method is called back when the Spring class loader fails to load, only considering classes.
        URL url = findResource(path, true);
        return url == null ? null : Collections.enumeration(Collections.singleton(url));
    }

    @Override
    public boolean test(String name) {
        return config.isEssential(name);
    }

    @Override
    public ResourcerType getType() {
        return ResourcerType.PLUGIN;
    }

    @Override
    public void close() {
        Close close = Close.instance();
        loaders.values().forEach(close::close);
        loaders.clear();
    }

    /**
     * Finds a class resource by path with configurable parent delegation.
     *
     * @param path   the resource path (must end with ".class")
     * @param parent whether to search in parent ClassLoaders
     * @return the resource URL if found, null otherwise
     */
    private URL findResource(String path, boolean parent) {
        if (path == null || !path.endsWith(".class")) {
            // not a class file
            return null;
        }
        String className = path.replace('/', '.');
        ResourceConfig rc = config.getPluginResource();
        if (rc.isSelf(className)) {
            // find in plugins
            for (Map.Entry<String, LiveClassLoader> entry : loaders.entrySet()) {
                URL url = entry.getValue().findResource(path);
                if (url != null) {
                    return url;
                }
            }
        } else if (parent && rc.isParent(className)) {
            ClassLoader loader = this.parent;
            while (loader instanceof LiveClassLoader) {
                URL url = ((LiveClassLoader) loader).findResource(path);
                if (url != null) {
                    return url;
                }
                loader = loader.getParent();
            }
        }
        return null;
    }
}
