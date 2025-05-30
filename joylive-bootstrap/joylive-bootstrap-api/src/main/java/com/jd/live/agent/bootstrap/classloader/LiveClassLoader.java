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

import lombok.Getter;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
    private final Map<String, ClassCache> caches = new ConcurrentHashMap<>(4096);

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
        if (!started.get()) {
            throw new ClassNotFoundException("class " + name + " is not found.");
        }
        // candidates for plugin classloader.
        ClassLoader[] candidates = filter == null ? null : filter.getCandidates();
        if (filter != null && filter.loadBySelf(name)) {
            return loadBySelf(getClassLoadingLock(name), name, resolve);
        } else if (filter != null && filter.loadByParent(name)) {
            return loadByParent(name, resolve, candidates);
        } else {
            // first candidature for plugin classloader, use the classloader of the enhanced type and thread context.
            if (candidates != null) {
                try {
                    return loadByClassLoader(candidates, name, resolve, c -> c != this);
                } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                    // ignore
                }
            }
            return loadByDefault(name, resolve);
        }
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
     * Attempts to load a class using the local class cache.
     *
     * @param mutex   the synchronization object to use for thread safety
     * @param name    the fully qualified name of the desired class
     * @param resolve if true, resolve the class (perform linking and verification)
     * @return the resulting Class object
     * @throws ClassNotFoundException if the class cannot be found in the cache
     */
    private Class<?> loadBySelf(Object mutex, String name, boolean resolve) throws ClassNotFoundException {
        ClassCache cache = findClass(name, mutex, resolve);
        Class<?> type = cache == null ? null : cache.getType();
        if (type != null) {
            return type;
        }
        throw new ClassNotFoundException("class " + name + " is not found.");
    }

    /**
     * Attempts to load a class using the parent class loader delegation model.
     *
     * @param name        the fully qualified name of the desired class
     * @param resolve     if true, resolve the class (perform linking and verification)
     * @param candidates the class loader to use for candidature
     * @return the resulting Class object
     * @throws ClassNotFoundException if the class cannot be found by the parent loader
     */
    private Class<?> loadByParent(String name, boolean resolve, ClassLoader[] candidates) throws ClassNotFoundException {
        ClassLoader parent = getParent();
        try {
            return loadByClassLoader(parent, name, resolve);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // javax.servlet.http.HttpServlet in tomcat-core
            // spring boot jar
            return loadByClassLoader(candidates, name, resolve, c -> c != parent);
        }
    }

    /**
     * Attempts to load a class using the default loading sequence (self-first then parent delegation).
     *
     * @param name    the fully qualified name of the desired class
     * @param resolve if true, resolves the class (performs linking and verification)
     * @return the loaded Class object
     * @throws ClassNotFoundException if the class cannot be found by either this loader or its parent
     */
    private Class<?> loadByDefault(String name, boolean resolve) throws ClassNotFoundException {
        // self
        Object mutex = getClassLoadingLock(name);
        Class<?> type;
        ClassCache cache = findClass(name, mutex, resolve);
        type = cache == null ? null : cache.getType();
        if (type == null) {
            // parent
            type = super.loadClass(name, resolve);
            if (type.getClassLoader() == this) {
                caches.putIfAbsent(name, new ClassCache(name, mutex, type, resolve));
            }
        }
        return type;
    }

    /**
     * Loads a class using the specified class loader with optional resolution.
     *
     * @param classLoaders the class loader to use for loading the class
     * @param name         the fully qualified name of the desired class
     * @param resolve      if true, resolve the class (perform linking and verification)
     * @return the resulting Class object
     * @throws ClassNotFoundException if the class cannot be found by the specified loader
     */
    private Class<?> loadByClassLoader(ClassLoader[] classLoaders, String name, boolean resolve, Predicate<ClassLoader> predicate) throws ClassNotFoundException {
        if (classLoaders != null) {
            for (ClassLoader classLoader : classLoaders) {
                if (classLoader != null && (predicate == null || predicate.test(classLoader))) {
                    try {
                        return loadByClassLoader(classLoader, name, resolve);
                    } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                    }
                }
            }
        }
        throw new ClassNotFoundException("class " + name + " is not found.");
    }

    /**
     * Loads a class using the specified class loader with optional resolution.
     *
     * @param classLoader the class loader to use for loading the class
     * @param name        the fully qualified name of the desired class
     * @param resolve     if true, resolve the class (perform linking and verification)
     * @return the resulting Class object
     * @throws ClassNotFoundException if the class cannot be found by the specified loader
     */
    private Class<?> loadByClassLoader(ClassLoader classLoader, String name, boolean resolve) throws ClassNotFoundException {
        Class<?> type = classLoader.loadClass(name);
        if (resolve) {
            resolveClass(type);
        }
        return type;
    }

    /**
     * Attempts to find a class in the cache, loading and caching it if not already present.
     *
     * @param name    The name of the class to load.
     * @param mutex   The lock of the classloader
     * @param resolve Whether to resolve the class or not
     * @return The loaded class, wrapped in a ClassCache object.
     */
    private ClassCache findClass(String name, Object mutex, boolean resolve) {
        ClassCache cache = caches.get(name);
        if (cache == null) {
            ClassCache newCache = null;
            synchronized (mutex) {
                cache = caches.get(name);
                if (cache == null) {
                    try {
                        Class<?> type = findClass(name);
                        if (resolve) {
                            resolveClass(type);
                        }
                        newCache = new ClassCache(name, mutex, type, resolve);
                    } catch (ClassNotFoundException e) {
                        return null;
                    }
                }
            }
            if (newCache != null) {
                caches.put(name, newCache);
                cache = newCache;
            }
        }
        return cache;
    }

    /**
     * A simple cache entry for storing class definitions.
     */
    private static class ClassCache {

        @Getter
        private final String name;

        @Getter
        private final Class<?> type;

        private final Object mutex;

        private volatile boolean resolved;

        ClassCache(String name, Object mutex, Callable<Class<?>> callable) {
            this(name, mutex, findClass(callable, mutex), null);
        }

        ClassCache(String name, Object mutex, Class<?> type, Boolean resolved) {
            this.name = name;
            this.mutex = mutex;
            this.type = type;
            this.resolved = resolved != null && resolved;
        }

        private static Class<?> findClass(Callable<Class<?>> callable, Object mutex) {
            synchronized (mutex) {
                try {
                    return callable.call();
                } catch (Exception ignored) {
                }
            }
            return null;
        }

        public void resolve(Consumer<Class<?>> resolver) {
            if (resolved) {
                return;
            }
            synchronized (mutex) {
                if (resolved) {
                    return;
                }
                resolver.accept(type);
                resolved = true;
            }
        }
    }
}
