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
package com.jd.live.agent.implement.bytekit.bytebuddy.util;

import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * module util
 *
 * @since 1.6.0
 */
public class ModuleUtil {

    private static final Map<JavaModule, Map<JavaModule, Set<String>>> MODULE_EXPORTS = new ConcurrentHashMap<>();

    /**
     * Exports packages from source modules to target modules.
     *
     * @param instrumentation the instrumentation object
     * @param targets         a map of target modules and their corresponding source types
     * @param defaultModule   the definition module
     * @param loaders         the class loaders
     */
    public static void export(Instrumentation instrumentation,
                              Map<String, Set<String>> targets,
                              JavaModule defaultModule,
                              ClassLoader... loaders) {
        for (Map.Entry<String, Set<String>> entry : targets.entrySet()) {
            JavaModule targetModule = entry.getKey() == null || entry.getKey().isEmpty()
                    ? defaultModule
                    : getModule(entry.getKey(), loaders);
            if (targetModule != null) {
                for (String sourceType : entry.getValue()) {
                    JavaModule sourceModule = getModule(sourceType, loaders);
                    if (sourceModule != null && !sourceModule.equals(targetModule)) {
                        Set<String> exported = MODULE_EXPORTS.computeIfAbsent(sourceModule, m -> new ConcurrentHashMap<>()).
                                computeIfAbsent(targetModule, m -> new CopyOnWriteArraySet<>());
                        int index = sourceType.lastIndexOf('.');
                        if (index > 0) {
                            String sourcePackage = sourceType.substring(0, index);
                            if (!isExportedAndOpen(sourceModule, sourcePackage, targetModule) && exported.add(sourcePackage)) {
                                addExportOrOpen(instrumentation, sourceModule, sourcePackage, targetModule);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Exports the specified package from the source module to the target module.
     *
     * @param instrumentation the instrumentation object used to modify the modules
     * @param sourceType      the type of the source module (e.g., "java.base")
     * @param sourcePackage   the package to export
     * @param targetType      the type of the target module (e.g., "java.se")
     * @param loaders         the class loaders to use when resolving the modules
     */
    public static void export(Instrumentation instrumentation, String sourceType, String sourcePackage, String targetType, ClassLoader... loaders) {
        JavaModule sourceModule = getModule(sourceType, loaders);
        JavaModule targetModule = getModule(targetType, loaders);
        if (sourceModule != null && targetModule != null) {
            Set<String> exported = MODULE_EXPORTS.computeIfAbsent(sourceModule, m -> new ConcurrentHashMap<>()).
                    computeIfAbsent(targetModule, m -> new CopyOnWriteArraySet<>());
            if (!isExportedAndOpen(sourceModule, sourcePackage, targetModule) && exported.add(sourcePackage)) {
                addExportOrOpen(instrumentation, sourceModule, sourcePackage, targetModule);
            }
        }
    }

    /**
     * Retrieves the {@link JavaModule} associated with a given class name, attempting to resolve
     * the class via the specified class loader. This method is used to find the module of a class
     * that needs to have packages exported or opened to another module.
     *
     * @param type    The fully qualified name of the class whose module is to be retrieved.
     * @param loaders The class loaders.
     * @return The {@link JavaModule} associated with the class, or the specified default module
     * if the class cannot be resolved.
     */
    private static JavaModule getModule(String type, ClassLoader... loaders) {
        type = type == null ? null : type.trim();
        if (type != null && !type.isEmpty() && loaders != null) {
            for (ClassLoader loader : loaders) {
                if (loader != null) {
                    try {
                        Class<?> exportType = Class.forName(type, false, loader);
                        return JavaModule.ofType(exportType);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks if a package is already exported and opened from one module to another. This is used
     * to avoid unnecessary module modifications if the access is already available.
     *
     * @param source      The module from which the package is to be exported or opened.
     * @param packageName The name of the package.
     * @param target      The module to which the package should be exported or opened.
     * @return {@code true} if the package is already exported or opened to the target module,
     * {@code false} otherwise.
     */
    private static boolean isExportedAndOpen(JavaModule source, String packageName, JavaModule target) {
        PackageDescription.Simple packageDescription = new PackageDescription.Simple(packageName);
        return source.isExported(packageDescription, target) && source.isOpened(packageDescription, target);
    }

    /**
     * Dynamically exports or opens a package from one module to another. This method uses the
     * {@link ClassInjector.UsingInstrumentation} to modify the module declarations at runtime,
     * allowing for increased flexibility in accessing internal APIs or injecting behavior.
     *
     * @param instrumentation The {@link Instrumentation} object provided by the Java agent mechanism.
     * @param source          The module from which the package is to be exported or opened.
     * @param packageName     The name of the package to export or open.
     * @param target          The module to which the package should be exported or opened.
     */
    private static void addExportOrOpen(Instrumentation instrumentation, JavaModule source, String packageName, JavaModule target) {
        ClassInjector.UsingInstrumentation.redefineModule(instrumentation,
                source,
                Collections.singleton(target),
                Collections.emptyMap(),
                Collections.singletonMap(packageName, Collections.singleton(target)),
                Collections.emptySet(),
                Collections.emptyMap());
    }
}
