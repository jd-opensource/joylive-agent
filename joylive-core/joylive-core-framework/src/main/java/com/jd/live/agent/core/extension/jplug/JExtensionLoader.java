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

import com.jd.live.agent.core.exception.PluginException;
import com.jd.live.agent.core.extension.ExtensionDesc;
import com.jd.live.agent.core.extension.ExtensionListener;
import com.jd.live.agent.core.extension.ExtensionLoader;
import com.jd.live.agent.core.extension.Name;
import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.extension.condition.ConditionMatcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Loads SPI (Service Provider Interface) extensions.
 */
public class JExtensionLoader implements ExtensionLoader {

    /**
     * Prefix for service provider configuration files.
     */
    protected static final String PREFIX = "META-INF/services/";

    /**
     * The class loader used to locate and load service providers.
     */
    private final ClassLoader classLoader;

    /**
     * The condition matcher used to filter service providers.
     */
    private final ConditionMatcher matcher;

    /**
     * The listener notified of extension loading events.
     */
    private final ExtensionListener listener;

    /**
     * The strategy for instantiating service provider instances.
     */
    private final Instantiation instantiation;

    /**
     * Constructs a new JExtensionLoader with the specified class loader, condition matcher, listener, and instantiation strategy.
     *
     * @param classLoader The class loader to use for finding service provider configurations.
     * @param matcher     The condition matcher for filtering service providers.
     * @param listener    The listener to notify of extension loading events.
     */
    public JExtensionLoader(ClassLoader classLoader, ConditionMatcher matcher, ExtensionListener listener) {
        this.classLoader = classLoader;
        this.matcher = matcher;
        this.listener = listener;
        this.instantiation = ClassInstantiation.INSTANCE;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public <T> List<ExtensionDesc<T>> load(final Class<T> extensible) {
        if (extensible == null) {
            return null;
        }
        List<ExtensionDesc<T>> result = new LinkedList<>();

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader[] loaders = getClassLoaders(extensible);
        String resource = getResource(extensible);
        for (ClassLoader loader : loaders) {
            if (loader != null) {
                try {
                    if (loader != contextClassLoader) {
                        Thread.currentThread().setContextClassLoader(loader);
                    }
                    // Load plugin names
                    Collection<String> classNames = loadPluginName(extensible, loader, resource);
                    if (!classNames.isEmpty()) {
                        Class<T> clazz;
                        // Iterate through plugins
                        for (String className : classNames) {
                            // Load class, filtering out those that don't meet the conditions
                            clazz = loadPluginClass(extensible, loader, className);
                            if (clazz != null) {
                                result.add(createExtension(extensible, clazz, loader));
                            }
                        }
                        break;
                    }
                } catch (IOException e) {
                    throw new PluginException("An error occurred while reading resource " + resource + ".", e);
                } finally {
                    if (loader != contextClassLoader) {
                        Thread.currentThread().setContextClassLoader(contextClassLoader);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Creates an extension descriptor for the given extension type.
     *
     * @param <T>        The type of the extension.
     * @param extensible The class object representing the extensible type.
     * @param type       The class object representing the extension implementation.
     * @param loader     The class loader to use for loading the extension.
     * @return An {@link ExtensionDesc} object representing the created extension.
     */
    private <T> ExtensionDesc<T> createExtension(final Class<T> extensible, final Class<T> type, final ClassLoader loader) {
        // Retrieve annotations from extensible type and extension implementation
        Extensible extensibleAnno = extensible.getAnnotation(Extensible.class);
        Extension extensionAnno = type.getAnnotation(Extension.class);

        // Create a new JExtension instance with the gathered information
        JExtension<T> e = new JExtension<>(
                new Name<>(extensible, extensibleAnno == null || isEmpty(extensibleAnno.value()) ? extensible.getName() : extensibleAnno.value()),
                new Name<>(type, extensionAnno == null || isEmpty(extensionAnno.value()) ? extensible.getName() : extensionAnno.value()),
                extensionAnno == null || isEmpty(extensionAnno.provider()) ? extensible.getName() : extensionAnno.provider(),
                extensionAnno != null ? extensionAnno.order() : Short.MAX_VALUE,
                extensionAnno == null || extensionAnno.singleton(),
                loader,
                instantiation,
                listener);
        return e;
    }

    protected boolean isEmpty(final String value) {
        return value == null || value.isEmpty();
    }

    /**
     * Retrieves the appropriate class loaders to use for loading extensions.
     *
     * @param extensible The extensible interface class.
     * @return An array of {@link ClassLoader} objects.
     */
    protected ClassLoader[] getClassLoaders(final Class<?> extensible) {
        return classLoader != null ? new ClassLoader[]{classLoader} :
                new ClassLoader[]{extensible.getClassLoader(), Thread.currentThread().getContextClassLoader()};
    }

    /**
     * Constructs the resource name for the given service.
     *
     * @param service The service class.
     * @return The resource name.
     */
    protected String getResource(final Class<?> service) {
        return PREFIX + service.getName();
    }

    /**
     * Loads plugin names from the specified resource.
     *
     * @param extensible The extensible interface class.
     * @param loader     The class loader to use for loading the resource.
     * @param resource   The resource name.
     * @return A collection of plugin names.
     * @throws IOException If an I/O error occurs.
     */
    protected Collection<String> loadPluginName(final Class<?> extensible, final ClassLoader loader, final String resource) throws IOException {
        Set<String> names = new LinkedHashSet<>();
        Enumeration<URL> resources = loader.getResources(resource);
        while (resources.hasMoreElements()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resources.nextElement().openStream(), StandardCharsets.UTF_8))) {
                loadPluginName(extensible, reader, names);
            }
        }
        return names;
    }

    /**
     * Loads plugin names from the given reader into the specified set.
     *
     * @param extensible The extensible interface class.
     * @param reader     The reader to read plugin names from.
     * @param names      The set to which plugin names will be added.
     * @throws IOException If an I/O error occurs.
     */
    protected void loadPluginName(final Class<?> extensible, final BufferedReader reader, final Set<String> names) throws IOException {
        String ln;
        int ci;
        int length;
        int cp;
        while ((ln = reader.readLine()) != null) {
            // Filter out comments
            ci = ln.indexOf('#');
            if (ci >= 0) {
                ln = ln.substring(0, ci);
            }
            // Trim whitespace
            ln = ln.trim();
            length = ln.length();
            if (length > 0) {
                for (int i = 0; i < length; i++) {
                    cp = ln.codePointAt(i);
                    if (i == 0 && !Character.isJavaIdentifierStart(cp)
                            || i > 0 && !Character.isJavaIdentifierPart(cp) && (cp != '.')) {
                        // Invalid Java class name
                        throw new PluginException(extensible.getName() + ": Illegal provider-class name: " + ln);
                    }
                }
                names.add(ln);
            }
        }
    }

    /**
     * Loads the plugin class with the given name using the specified class loader.
     *
     * @param <T>        The type of the plugin interface.
     * @param extensible The class object representing the plugin interface.
     * @param loader     The class loader to use for loading the plugin class.
     * @param className  The name of the plugin class to load.
     * @return The loaded plugin class.
     */
    @SuppressWarnings("unchecked")
    protected <T> Class<T> loadPluginClass(final Class<T> extensible, final ClassLoader loader, final String className) {
        Class<?> result;
        try {
            result = loader.loadClass(className);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            throw new PluginException("Extension " + className + " is not found.");
        }
        if (!extensible.isAssignableFrom(result)) {
            // Invalid Java class
            throw new PluginException("Extension " + className + " is not a subtype of " + extensible.getName());
        } else if (result.isInterface() || Modifier.isAbstract(result.getModifiers()) || !Modifier.isPublic(result.getModifiers())) {
            // Invalid Java class
            throw new PluginException("Extension " + className + " is not an implement of " + extensible.getName());
        }
        return matcher == null || matcher.match(result, loader) ? (Class<T>) result : null;
    }

}
