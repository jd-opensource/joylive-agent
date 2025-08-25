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

import com.jd.live.agent.bootstrap.util.Inclusion;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
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

    public static final ThreadLocal<CandidateFeature> CONTEXT_LOADER_ENABLED = ThreadLocal.withInitial(CandidateFeature::new);

    public static ClassLoader BOOT_CLASS_LOADER = null;

    public static ClassLoader APP_CLASS_LOADER = null;

    private static final Map<ClassLoader, ClassResolver> resolvers = new ConcurrentHashMap<>();

    /**
     * The type of resources this class loader is concerned with.
     */
    private final ResourcerType type;

    private final ResourceConfig config;

    private final Inclusion bootstrap;

    private final File configPath;

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

    @Getter
    private final Set<String> packageNames = new HashSet<>();

    /**
     * Constructs a new LiveClassLoader with the specified URLs, parent class loader, type, and filter.
     *
     * @param urls       The URLs from which to load classes and resources.
     * @param parent     The parent class loader for delegation.
     * @param type       The type of resources to manage.
     * @param config     The configuration for resource management.
     * @param configPath The config directory.
     */
    public LiveClassLoader(URL[] urls, ClassLoader parent, ResourcerType type, ResourceConfig config, Inclusion bootstrap, File configPath) {
        this(urls, parent, type, config, bootstrap, configPath, null);
    }

    /**
     * Constructs a new LiveClassLoader with the specified URLs, parent class loader, type, filter, and name.
     *
     * @param urls       The URLs from which to load classes and resources.
     * @param parent The parent class loader for delegation.
     * @param type   The type of resources to manage.
     * @param config The configuration for resource management.
     * @param configPath The config directory.
     * @param name   The name of the class loader. If {@code null} or empty, the name is derived from the resource type.
     */
    public LiveClassLoader(URL[] urls, ClassLoader parent, ResourcerType type, ResourceConfig config, Inclusion bootstrap, File configPath, String name) {
        super(urls, parent);
        this.type = type;
        this.config = config;
        this.bootstrap = bootstrap;
        this.configPath = configPath;
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
        return loadClass(name, resolve, null);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve, Predicate<ClassLoader> predicate) throws ClassNotFoundException {
        if (!started.get()) {
            throw new ClassNotFoundException("class " + name + " is not found.");
        } else if (config.isSelf(name)) {
            return loadBySelf(name, resolve, predicate);
        } else if (config.isParent(name)) {
            return loadByParent(name, resolve, predicate);
        } else {
            ClassLoader[] candidates = type.fallback() ? getFallbacks() : null;
            if (candidates != null && candidates.length > 0) {
                return loadByCandidate(candidates, name, resolve, c -> c != this && (predicate == null || predicate.test(c)));
            }
            return loadByRoot(name, resolve, predicate);
        }
    }

    @Override
    public Class<?> findClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!started.get()) {
            throw new ClassNotFoundException("class " + name + " is not found.");
        }
        return loadBySelf(name, resolve, null);
    }

    @Override
    public URL getResource(String name) {
        if (config.isConfig(name)) {
            return getConfigFile(name);
        } else if (config.isIsolation(name)) {
            return findResource(name);
        }
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (config.isConfig(name)) {
            URL url = getConfigFile(name);
            return url == null ? Collections.emptyEnumeration() : Collections.enumeration(Collections.singleton(url));
        } else if (config.isIsolation(name)) {
            return findResources(name);
        }
        return super.getResources(name);
    }

    @Override
    public ResourcerType getType() {
        return type;
    }

    public void addPackage(String packageName) {
        if (packageName != null && !packageName.isEmpty()) {
            packageNames.add(packageName);
        }
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

    public static CandidateFeature getCandidateFeature() {
        return CONTEXT_LOADER_ENABLED.get();
    }

    /**
     * Attempts to load a class using the local class cache.
     *
     * @param name      the fully qualified name of the desired class
     * @param resolve   if true, resolve the class (perform linking and verification)
     * @param predicate the predicate to determine if the class loader should be used
     * @return the resulting Class object
     * @throws ClassNotFoundException if the class cannot be found in the cache
     */
    private Class<?> loadBySelf(String name, boolean resolve, Predicate<ClassLoader> predicate) throws ClassNotFoundException {
        if (predicate == null || predicate.test(this)) {
            ClassCache cache = findClassCache(name, resolve);
            Class<?> type = cache == null ? null : cache.getType();
            if (type != null) {
                return type;
            }
        }
        throw new ClassNotFoundException("class " + name + " is not found by " + getType());
    }

    /**
     * Attempts to load a class using the parent class loader delegation model.
     *
     * @param name      the fully qualified name of the desired class
     * @param resolve   if true, resolve the class (perform linking and verification)
     * @param predicate the predicate to determine if the class loader should be used
     * @return the resulting Class object
     * @throws ClassNotFoundException if the class cannot be found by the parent loader
     */
    private Class<?> loadByParent(String name, boolean resolve, Predicate<ClassLoader> predicate) throws ClassNotFoundException {
        ClassLoader parent = getParent();
        if (!bootstrap.test(name)) {
            while (parent instanceof LiveClassLoader && (predicate == null || predicate.test(parent))) {
                ClassCache cache = ((LiveClassLoader) parent).findClassCache(name, resolve);
                if (cache != null) {
                    // class is resolved by findClassCache
                    return cache.getType();
                }
                parent = parent.getParent();
            }
            throw new ClassNotFoundException("class " + name + " is not found by " + getType());
        } else {
            return loadByRoot(name, resolve, predicate);
        }
    }

    /**
     * Loads a class using candidate ClassLoaders based on the given predicate.
     *
     * @param candidates the array of ClassLoaders to try
     * @param name       the fully qualified class name
     * @param resolve    whether to resolve the class
     * @param predicate  the condition to determine which ClassLoader to use
     * @return the loaded Class object
     * @throws ClassNotFoundException if the class cannot be found by any candidate ClassLoader
     */
    private Class<?> loadByCandidate(ClassLoader[] candidates, String name, boolean resolve, Predicate<ClassLoader> predicate) throws ClassNotFoundException {
        // candidates for plugin classloader.
        Class<?> type;
        for (ClassLoader candidate : candidates) {
            type = loadClass(candidate, name, resolve, predicate);
            if (type != null) {
                return type;
            }
        }
        throw new ClassNotFoundException("class " + name + " is not found by " + getType());
    }

    /**
     * Loads a class using root classloader based on the given predicate.
     *
     * @param name      the fully qualified class name
     * @param resolve   whether to resolve the class
     * @param predicate the condition to determine which ClassLoader to use
     * @return the loaded Class object
     * @throws ClassNotFoundException if the class cannot be found by any candidate ClassLoader
     */
    private Class<?> loadByRoot(String name, boolean resolve, Predicate<ClassLoader> predicate) throws ClassNotFoundException {
        // not live agent class.
        ClassLoader parent = getParent();
        while (parent instanceof LiveClassLoader) {
            parent = parent.getParent();
        }
        Class<?> type = loadClass(parent, name, resolve, predicate);
        if (type != null) {
            return type;
        }
        throw new ClassNotFoundException("class " + name + " is not found by " + getType());
    }

    /**
     * Loads a class using the specified class loader with optional resolution.
     *
     * @param classLoader the class loader to use for loading the class
     * @param name        the fully qualified name of the desired class
     * @param resolve     if true, resolve the class (perform linking and verification)
     * @param predicate   the predicate to determine if the class loader should be used
     * @return the resulting Class object
     */
    private Class<?> loadClass(ClassLoader classLoader, String name, boolean resolve, Predicate<ClassLoader> predicate) {
        if (classLoader != null && (predicate == null || predicate.test(classLoader))) {
            try {
                Class<?> type = classLoader.loadClass(name);
                if (resolve) {
                    // resolve class
                    resolvers.computeIfAbsent(type.getClassLoader(), ClassResolver::new).resolve(type);
                }
                return type;
            } catch (ClassNotFoundException | LinkageError ignored) {
            }
        }
        return null;
    }

    /**
     * Gets candidate class loaders for fallback loading.
     *
     * @return array of candidate class loaders, or null if none available
     */
    private ClassLoader[] getFallbacks() {
        CandidateFeature feature = getCandidateFeature();
        if (feature == null || !feature.isContextLoaderEnabled()) {
            return null;
        }
        // The thread context class loader may be inconsistent with the framework's boot class loader.
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        contextClassLoader = contextClassLoader == LiveClassLoader.APP_CLASS_LOADER || !feature.test(contextClassLoader) ? null : contextClassLoader;
        ClassLoader bootClassLoader = LiveClassLoader.BOOT_CLASS_LOADER == LiveClassLoader.APP_CLASS_LOADER || LiveClassLoader.BOOT_CLASS_LOADER == contextClassLoader || !feature.test(LiveClassLoader.BOOT_CLASS_LOADER) ? null : LiveClassLoader.BOOT_CLASS_LOADER;
        if (bootClassLoader == null) {
            return contextClassLoader == null ? null : new ClassLoader[]{contextClassLoader};
        } else if (contextClassLoader == null) {
            return new ClassLoader[]{bootClassLoader};
        }
        return new ClassLoader[]{LiveClassLoader.BOOT_CLASS_LOADER, contextClassLoader};
    }

    /**
     * Attempts to find a class in the cache, loading and caching it if not already present.
     *
     * @param name    The name of the class to load.
     * @param resolve Whether to resolve the class or not
     * @return The loaded class, wrapped in a ClassCache object.
     */
    private ClassCache findClassCache(String name, boolean resolve) {
        ClassCache cache = caches.get(name);
        if (cache == null) {
            Locker locker = new Locker(getClassLoadingLock(name));
            cache = locker.callQuietly(() -> {
                ClassCache c = caches.get(name);
                if (c == null) {
                    Class<?> type = findClass(name);
                    if (resolve) {
                        resolveClass(type);
                    }
                    c = new ClassCache(name, locker, type, resolve);
                    caches.putIfAbsent(name, c);
                }
                return c;
            });
        }
        if (cache != null && resolve) {
            cache.resolve(this::resolveClass);
        }
        return cache;
    }

    private URL getConfigFile(String name) {
        File file = new File(configPath, name);
        if (file.exists() && file.isFile()) {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException ignore) {
            }
        }
        return null;
    }

    /**
     * A simple cache entry for storing class definitions.
     */
    private static class ClassCache {

        @Getter
        private final String name;

        @Getter
        private final Class<?> type;

        private final Locker locker;

        private volatile boolean resolved;

        ClassCache(String name, Locker locker, Class<?> type, boolean resolved) {
            this.name = name;
            this.locker = locker;
            this.type = type;
            this.resolved = resolved;
        }

        public void resolve(Consumer<Class<?>> resolver) {
            if (resolved) {
                return;
            }
            locker.run(() -> {
                try {
                    resolver.accept(type);
                } catch (Throwable ignored) {
                    // java.lang.LinkageError
                }
                resolved = true;
            });
        }
    }

    private static class Locker {

        private final Object mutex;

        Locker(Object mutex) {
            this.mutex = mutex;
        }

        public void run(Runnable runnable) {
            synchronized (mutex) {
                runnable.run();
            }
        }

        public <T> T callQuietly(Callable<T> callable) {
            synchronized (mutex) {
                try {
                    return callable.call();
                } catch (Exception e) {
                    return null;
                }
            }
        }
    }

    /**
     * A utility class for safely resolving classes using ClassLoader's internal locking mechanism.
     */
    private static class ClassResolver {

        private final ClassLoader classLoader;

        private final Method lockMethod;

        private final Method resolveMethod;

        ClassResolver(ClassLoader classLoader) {
            this.classLoader = classLoader;
            this.lockMethod = getLockMethod(classLoader.getClass());
            this.resolveMethod = getResolveMethod(classLoader.getClass());
        }

        /**
         * Resolves the specified class using proper synchronization.
         *
         * @param type the Class to resolve
         */
        public void resolve(Class<?> type) {
            if (lockMethod == null || resolveMethod == null) {
                return;
            }
            try {
                synchronized (lockMethod.invoke(classLoader, type.getName())) {
                    resolveMethod.invoke(classLoader, type);
                }
            } catch (Throwable ignored) {
            }
        }

        /**
         * Locates the getClassLoadingLock method in the ClassLoader hierarchy.
         *
         * @param loaderType the ClassLoader class to search
         * @return the getClassLoadingLock Method, or null if not found
         */
        private static Method getLockMethod(Class<?> loaderType) {
            // find getClassLoadingLock method of classloader
            while (loaderType != null && loaderType != Object.class) {
                try {
                    Method method = loaderType.getDeclaredMethod("getClassLoadingLock", String.class);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException e) {
                    loaderType = loaderType.getSuperclass();
                }
            }
            return null;
        }

        /**
         * Locates the resolveClass method in the ClassLoader hierarchy.
         *
         * @param loaderType the ClassLoader class to search
         * @return the resolveClass Method, or null if not found
         */
        private static Method getResolveMethod(Class<?> loaderType) {
            // find resolveClass method of classloader
            while (loaderType != null && loaderType != Object.class) {
                try {
                    Method method = loaderType.getDeclaredMethod("resolveClass", Class.class);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException e) {
                    loaderType = loaderType.getSuperclass();
                }
            }
            return null;
        }
    }
}
