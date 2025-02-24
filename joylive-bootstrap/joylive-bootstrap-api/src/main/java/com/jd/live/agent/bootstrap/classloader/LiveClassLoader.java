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
package com.jd.live.agent.bootstrap.classloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A class loader that supports dynamic loading of classes and resources from URLs,
 * with additional capabilities for resource management and caching.
 */
public class LiveClassLoader extends URLClassLoader implements URLResourcer {

    /**
     * The type of resources this class loader is concerned with.
     */
    private final ResourcerType type;

    /**
     * The filter used to determine how resources are loaded.
     */
    private final ResourceFilter filter;

    /**
     * The name of this class loader, potentially derived from the resource type.
     */
    private final String name;

    /**
     * A cache for storing loaded class definitions to avoid redundant loading.
     */
    private final ConcurrentMap<String, ClassCache> caches = new ConcurrentHashMap<>(4096);

    /**
     * Flag indicating whether this class loader has been started.
     */
    private final AtomicBoolean started = new AtomicBoolean(true);

    /**
     * Constructs a new LiveClassLoader with the specified URLs, type, and filter.
     *
     * @param urls   The URLs from which to load classes and resources.
     * @param type   The type of resources to manage.
     * @param filter The filter to apply when loading resources.
     */
    public LiveClassLoader(URL[] urls, ResourcerType type, ResourceFilter filter) {
        this(urls, null, type, filter, null);
    }

    /**
     * Constructs a new LiveClassLoader with the specified URLs, parent class loader, type, and filter.
     *
     * @param urls   The URLs from which to load classes and resources.
     * @param parent The parent class loader for delegation.
     * @param type   The type of resources to manage.
     * @param filter The filter to apply when loading resources.
     */
    public LiveClassLoader(URL[] urls, ClassLoader parent, ResourcerType type, ResourceFilter filter) {
        this(urls, parent, type, filter, null);
    }

    /**
     * Constructs a new LiveClassLoader with the specified URLs, parent class loader, type, filter, and name.
     *
     * @param urls   The URLs from which to load classes and resources.
     * @param parent The parent class loader for delegation.
     * @param type   The type of resources to manage.
     * @param filter The filter to apply when loading resources.
     * @param name   The name of the class loader. If {@code null} or empty, the name is derived from the resource type.
     */
    public LiveClassLoader(URL[] urls, ClassLoader parent, ResourcerType type, ResourceFilter filter, String name) {
        super(urls, parent);
        this.type = type;
        this.filter = filter;
        this.name = (name == null || name.isEmpty()) && type != null ? type.getName() : name;
    }

    @Override
    public String getId() {
        return name;
    }

    /**
     * Attempts to find a class in the cache, loading and caching it if not already present.
     *
     * @param name The name of the class to load.
     * @return The loaded class.
     */
    protected Class<?> findClassWithCache(String name) {
        ClassCache cache = caches.get(name);
        if (cache == null) {
            try {
                cache = new ClassCache(findClass(name));
            } catch (ClassNotFoundException ignored) {
                cache = ClassCache.EMPTY;
            }
            caches.put(name, cache);
        }
        return cache.type;
    }

    @Override
    public void add(URL... urls) {
        if (urls != null) {
            for (URL url : urls) {
                addURL(url);
            }
        }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return this.loadClass(name, false);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return this.loadClass(name, resolve, null);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve, CandidatorProvider candidatorProvider) throws ClassNotFoundException {
        if (!started.get()) {
            throw new ClassNotFoundException("class " + name + " is not found.");
        }

        ClassCache cache = caches.get(name);
        if (cache != null && cache.type != null) {
            if (resolve) {
                resolveClass(cache.type);
            }
            return cache.type;
        }

        Class<?> clazz;
        synchronized (getClassLoadingLock(name)) {
            cache = caches.get(name);
            if (cache != null && cache.type != null) {
                clazz = cache.type;
            } else {
                if (filter != null && !filter.loadByParent(name)) {
                    clazz = findClass(name);
                } else {
                    try {
                        clazz = super.loadClass(name, resolve);
                    } catch (ClassNotFoundException e) {
                        ClassLoader candidature = filter == null ? null : filter.getCandidator();
                        if (candidature != null && candidature != this && candidature != this.getParent()) {
                            clazz = candidature.loadClass(name);
                        } else {
                            throw e;
                        }
                    }
                    caches.put(name, new ClassCache(clazz));
                }
            }

            if (resolve) {
                resolveClass(clazz);
            }
        }

        return clazz;
    }

    @Override
    public URL getResource(String name) {
        URL url = filter == null ? null : filter.getResource(name, this);
        return url != null ? url : super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> urls = filter == null ? null : filter.getResources(name, this);
        return urls != null ? urls : super.getResources(name);
    }

    @Override
    public ResourcerType getType() {
        return type;
    }

    @Override
    public void close() throws IOException {
        if (started.compareAndSet(true, false)) {
            caches.clear();
            super.close();
        }
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * A simple cache entry for storing class definitions.
     */
    protected static class ClassCache {

        public static final ClassCache EMPTY = new ClassCache(null);

        protected Class<?> type;

        public ClassCache(Class<?> type) {
            this.type = type;
        }
    }
}
