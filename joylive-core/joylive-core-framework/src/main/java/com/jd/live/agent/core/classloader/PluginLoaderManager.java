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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.jd.live.agent.core.util.StringUtils.getPackage;

/**
 * Manages plugin class loaders and provides fallback support for external framework class loading failures.
 *
 * @since 1.0.0
 */
public class PluginLoaderManager implements ClassLoaderSupervisor, Resourcer, Closeable {

    private final ClassLoaderConfig config;

    private final LiveClassLoader parent;

    private final ClassLoaderFactory builder;

    private final Map<String, LiveClassLoader> names = new ConcurrentHashMap<>(100);

    private final Object mutex = new Object();

    private volatile Map<String, LiveClassLoader> packages;

    public PluginLoaderManager(ClassLoaderConfig config, LiveClassLoader parent, ClassLoaderFactory builder) {
        this.config = config;
        this.parent = parent;
        this.builder = builder;
    }

    @Override
    public ClassLoader get(String name) {
        return name == null || name.isEmpty() ? null : names.get(name);
    }

    @Override
    public ClassLoader remove(String name) {
        return name == null || name.isEmpty() ? null : names.remove(name);
    }

    @Override
    public LiveClassLoader create(String name) {
        LiveClassLoader loader = name == null || name.isEmpty() ? null : builder.create(name);
        if (loader != null) {
            names.put(name, loader);
        }
        return loader;
    }

    @Override
    public LiveClassLoader create(String name, URL[] urls) {
        LiveClassLoader loader = name == null || name.isEmpty() ? null : builder.create(name, urls);
        if (loader != null) {
            names.put(name, loader);
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
            return findClass(name, resolve, predicate);
        } else if (rc.isParent(name)) {
            return parent.loadClass(name, resolve, predicate);
        }
        throw new ClassNotFoundException("class " + name + " is not found.");
    }

    @Override
    public Class<?> findClass(String name, boolean resolve) throws ClassNotFoundException {
        return findClass(name, resolve, null);
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
        names.values().forEach(close::close);
        names.clear();
    }

    /**
     * Finds a class with the given predicate filter.
     *
     * @param name      the class name
     * @param resolve   whether to resolve the class
     * @param predicate the predicate to filter class loaders
     * @return the class if found
     * @throws ClassNotFoundException if the class is not found
     */
    private Class<?> findClass(String name, boolean resolve, Predicate<ClassLoader> predicate) throws ClassNotFoundException {
        CandidateFeature feature = LiveClassLoader.getCandidateFeature();
        // disable context loader
        return feature.disableAndRun(() -> {
            Class<?> type = findBySelf(name, predicate, loader -> loader.findClassQuietly(name, resolve));
            if (type != null) {
                return type;
            }
            throw new ClassNotFoundException("class " + name + " is not found.");
        });
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
            return findBySelf(path, className);
        } else if (parent && rc.isParent(className)) {
            return findByParent(path);
        }
        return null;
    }

    /**
     * Finds resource using self class loaders.
     *
     * @param path      the resource path
     * @param className the class name
     * @return the resource URL if found, null otherwise
     */
    private URL findBySelf(String path, String className) {
        return findBySelf(className, null, loader -> loader.findResource(path));
    }

    /**
     * Finds result using self class loaders by applying the given function.
     *
     * @param <T>       the result type
     * @param name      the class name
     * @param predicate the predicate to filter class loaders
     * @param func      the function to apply on class loaders
     * @return the result if found, null otherwise
     */
    private <T> T findBySelf(String name, Predicate<ClassLoader> predicate, Function<LiveClassLoader, T> func) {
        LiveClassLoader loader = findLoader(name);
        if (loader != null) {
            return (predicate == null || predicate.test(loader)) ? func.apply(loader) : null;
        }
        for (Map.Entry<String, LiveClassLoader> entry : names.entrySet()) {
            loader = entry.getValue();
            T t = (predicate == null || predicate.test(loader)) ? func.apply(loader) : null;
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    /**
     * Finds resource using parent class loaders.
     *
     * @param path the resource path
     * @return the resource URL if found, null otherwise
     */
    private URL findByParent(String path) {
        ClassLoader loader = this.parent;
        while (loader instanceof LiveClassLoader) {
            URL url = ((LiveClassLoader) loader).findResource(path);
            if (url != null) {
                return url;
            }
            loader = loader.getParent();
        }
        return null;
    }

    /**
     * Finds the appropriate class loader for the given class name.
     *
     * @param className the fully qualified class name
     * @return the class loader, or null if not found
     */
    private LiveClassLoader findLoader(String className) {
        String packageName = getPackage(className);
        LiveClassLoader loader = getClassLoader(packageName);
        return loader == null ? getClassLoader(getPackage(packageName)) : loader;
    }

    /**
     * Gets the class loader for the specified package.
     *
     * @param packageName the package name
     * @return the class loader, or null if not found
     */
    private LiveClassLoader getClassLoader(String packageName) {
        Map<String, LiveClassLoader> map = packages;
        if (map == null) {
            synchronized (mutex) {
                map = packages;
                if (map == null) {
                    map = new HashMap<>();
                    for (Map.Entry<String, LiveClassLoader> entry : names.entrySet()) {
                        LiveClassLoader loader = entry.getValue();
                        for (String p : loader.getPackageNames()) {
                            map.put(p, loader);
                        }
                    }
                    packages = map;
                }
            }
        }
        return map.get(packageName);
    }
}
