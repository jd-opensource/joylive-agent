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
package com.jd.live.agent.implement.bytekit.bytebuddy.plugin;

import com.jd.live.agent.bootstrap.bytekit.advice.AdviceDesc;
import com.jd.live.agent.bootstrap.bytekit.advice.AdviceHandler;
import com.jd.live.agent.bootstrap.bytekit.advice.AdviceKey;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.plugin.definition.Interceptor;
import com.jd.live.agent.core.extension.condition.ConditionMatcher;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDeclare;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginImporter;
import com.jd.live.agent.implement.bytekit.bytebuddy.advice.ConstructorAdvice;
import com.jd.live.agent.implement.bytekit.bytebuddy.advice.MemberMethodAdvice;
import com.jd.live.agent.implement.bytekit.bytebuddy.advice.StaticMethodAdvice;
import com.jd.live.agent.implement.bytekit.bytebuddy.type.BuddyMethodDesc;
import com.jd.live.agent.implement.bytekit.bytebuddy.type.BuddyTypeDesc;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.nullability.MaybeNull;
import net.bytebuddy.utility.nullability.NeverNull;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.jd.live.agent.core.extension.condition.ConditionMatcher.DEPEND_ON_LOADER;

/**
 * A transformer that modifies the bytecode of classes loaded by the JVM, based on plugins that
 * declare the need to import internal classes or modify method behavior. It implements both the
 * {@link AgentBuilder.RawMatcher} for matching classes to be transformed and {@link AgentBuilder.Transformer}
 * for applying the transformations.
 *
 * @since 1.0.0
 */
public class PluginTransformer implements AgentBuilder.RawMatcher, AgentBuilder.Transformer {

    private static final Logger logger = LoggerFactory.getLogger(PluginTransformer.class);

    private static final Map<JavaModule, Map<JavaModule, Set<String>>> MODULE_EXPORTS = new ConcurrentHashMap<>();

    private final Map<String, Boolean> definitionEnabled = new ConcurrentHashMap<>();

    private final Instrumentation instrumentation;

    private final PluginDeclare plugin;

    private final ConditionMatcher conditionMatcher;

    private final Map<String, List<InterceptorDefinition>> types = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code PluginTransformer} with the specified instrumentation,
     * plugin declaration, and condition matcher.
     *
     * @param instrumentation  the {@link Instrumentation} object provided by the Java agent mechanism.
     * @param plugin           the plugin declaration that contains the definitions for class and method
     *                         transformations.
     * @param conditionMatcher the condition matcher used to determine if a plugin definition should be
     *                         applied based on the current execution context.
     */
    public PluginTransformer(Instrumentation instrumentation,
                             PluginDeclare plugin,
                             ConditionMatcher conditionMatcher) {
        this.instrumentation = instrumentation;
        this.plugin = plugin;
        this.conditionMatcher = conditionMatcher;
    }

    @Override
    public boolean matches(@NeverNull TypeDescription description,
                           @MaybeNull ClassLoader loader,
                           @MaybeNull JavaModule module,
                           @MaybeNull Class<?> type,
                           @NeverNull ProtectionDomain domain) {
        if (plugin == null || plugin.isEmpty()) {
            return false;
        }

        String uniqueName = getUniqueName(description, loader);
        if (types.containsKey(uniqueName)) {
            return true;
        }

        BuddyTypeDesc typeDesc = new BuddyTypeDesc(description);
        List<InterceptorDefinition> matched = new ArrayList<>();
        for (PluginDefinition definition : plugin.getDefinitions()) {
            // match the type
            if (definition.getMatcher().match(typeDesc)) {
                // determine whether the plugin is enabled in this classloader.
                if (definitionEnabled.computeIfAbsent(getUniqueName(definition.getClass(), loader),
                        n -> conditionMatcher.match(definition.getClass(), loader, DEPEND_ON_LOADER))) {
                    Collections.addAll(matched, definition.getInterceptors());
                    // export internal java module packages to plugin
                    export(loader, module, definition);
                }
            }
        }
        return !matched.isEmpty() && types.putIfAbsent(uniqueName, matched) == null;
    }

    /**
     * Exports or opens packages dynamically from one module to another based on the plugin definitions.
     * This method allows classes defined by plugins to access internal packages of other modules that
     * are not exported or opened by default.
     *
     * @param loader     The class loader associated with the module to be modified.
     * @param module     The module from which packages are to be exported or opened.
     * @param definition The plugin definition that may require access to internal packages.
     */
    private void export(ClassLoader loader, JavaModule module, PluginDefinition definition) {
        if (module != JavaModule.UNSUPPORTED && definition instanceof PluginImporter) {
            JavaModule definitionModule = JavaModule.ofType(definition.getClass());
            String[] imports = ((PluginImporter) definition).getImports();
            if (definitionModule != null && imports != null) {
                for (String type : imports) {
                    JavaModule exportModule = getModule(loader, module, type);
                    if (exportModule != null) {
                        Set<String> exported = MODULE_EXPORTS.computeIfAbsent(exportModule, m -> new ConcurrentHashMap<>()).
                                computeIfAbsent(definitionModule, m -> new CopyOnWriteArraySet<>());
                        int index = type.lastIndexOf('.');
                        if (index > 0) {
                            String packageName = type.substring(0, index);
                            if (!isExportedOrOpen(exportModule, packageName, definitionModule) && exported.add(packageName)) {
                                addExportOrOpen(exportModule, packageName, definitionModule);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Generates a unique name for a type or class based on its description and class loader.
     * This unique name is used to identify classes across different class loaders.
     *
     * @param description The type description of the class.
     * @param loader      The class loader loading the class; may be {@code null} for the bootstrap class loader.
     * @return A unique name for the class, combining its full name and class loader.
     */
    private String getUniqueName(@NeverNull TypeDescription description, @MaybeNull ClassLoader loader) {
        return loader == null ? description.getActualName() : (description.getActualName() + "@" + loader);
    }

    /**
     * Overloaded version of {@code getUniqueName} that generates a unique name for a class based on
     * its {@link Class} object and class loader.
     *
     * @param type   The class for which to generate a unique name.
     * @param loader The class loader loading the class; may be {@code null} for the bootstrap class loader.
     * @return A unique name for the class, combining its full name and class loader.
     */
    private String getUniqueName(@NeverNull Class<?> type, @MaybeNull ClassLoader loader) {
        return loader == null ? type.getName() : (type.getName() + "@" + loader);
    }

    /**
     * Retrieves the {@link JavaModule} associated with a given class name, attempting to resolve
     * the class via the specified class loader. This method is used to find the module of a class
     * that needs to have packages exported or opened to another module.
     *
     * @param loader The class loader to use for class resolution.
     * @param module The default module to return in case the class cannot be resolved.
     * @param type   The fully qualified name of the class whose module is to be retrieved.
     * @return The {@link JavaModule} associated with the class, or the specified default module
     * if the class cannot be resolved.
     */
    private JavaModule getModule(ClassLoader loader, JavaModule module, String type) {
        type = type == null ? null : type.trim();
        if (type != null && !type.isEmpty()) {
            try {
                Class<?> exportType = Class.forName(type, false, loader);
                return JavaModule.ofType(exportType);
            } catch (ClassNotFoundException e) {
                return null;
            } catch (Throwable e) {
                return module;
            }
        }
        return module;
    }

    /**
     * Checks if a package is already exported or opened from one module to another. This is used
     * to avoid unnecessary module modifications if the access is already available.
     *
     * @param source      The module from which the package is to be exported or opened.
     * @param packageName The name of the package.
     * @param target      The module to which the package should be exported or opened.
     * @return {@code true} if the package is already exported or opened to the target module,
     * {@code false} otherwise.
     */
    private boolean isExportedOrOpen(JavaModule source, String packageName, JavaModule target) {
        return source.isExported(new PackageDescription.Simple(packageName), target);
    }

    /**
     * Dynamically exports or opens a package from one module to another. This method uses the
     * {@link ClassInjector.UsingInstrumentation} to modify the module declarations at runtime,
     * allowing for increased flexibility in accessing internal APIs or injecting behavior.
     *
     * @param source      The module from which the package is to be exported or opened.
     * @param packageName The name of the package to export or open.
     * @param target      The module to which the package should be exported or opened.
     */
    private void addExportOrOpen(JavaModule source, String packageName, JavaModule target) {
        ClassInjector.UsingInstrumentation.redefineModule(instrumentation,
                source,
                Collections.singleton(target),
                Collections.emptyMap(),
                Collections.singletonMap(packageName, Collections.singleton(target)),
                Collections.emptySet(),
                Collections.emptyMap());
    }

    @Override
    public DynamicType.Builder<?> transform(@NeverNull DynamicType.Builder<?> builder,
                                            @NeverNull TypeDescription description,
                                            @MaybeNull ClassLoader loader,
                                            @MaybeNull JavaModule module,
                                            @NeverNull ProtectionDomain domain) {
        List<InterceptorDefinition> interceptorDefinitions = types.get(getUniqueName(description, loader));
        if (interceptorDefinitions == null || interceptorDefinitions.isEmpty()) {
            return builder;
        }

        DynamicType.Builder<?> newBuilder = builder;
        List<Interceptor> interceptors;
        for (MethodDescription.InDefinedShape methodDesc : description.getDeclaredMethods()) {
            if (methodDesc.isNative() || methodDesc.isAbstract()) {
                continue;
            }
            interceptors = getInterceptors(methodDesc, interceptorDefinitions);
            if (!interceptors.isEmpty()) {
                String adviceKey;
                BuddyMethodDesc desc = new BuddyMethodDesc(methodDesc);
                String key = desc.getDescription();
                try {
                    if (methodDesc.isStatic()) {
                        adviceKey = AdviceKey.getMethodKey(key, desc.getActualName(), loader);
                        newBuilder = enhanceMethod(newBuilder, methodDesc, loader, interceptors, StaticMethodAdvice.class, adviceKey);
                    } else if (methodDesc.isConstructor()) {
                        adviceKey = AdviceKey.getConstructorKey(key, loader);
                        newBuilder = enhanceMethod(newBuilder, methodDesc, loader, interceptors, ConstructorAdvice.class, adviceKey);
                    } else {
                        adviceKey = AdviceKey.getMethodKey(key, desc.getActualName(), loader);
                        newBuilder = enhanceMethod(newBuilder, methodDesc, loader, interceptors, MemberMethodAdvice.class, adviceKey);
                    }
                } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException |
                         InvocationTargetException e) {
                    logger.warn("failed to enhance " + key + " , caused by " + e.getMessage());
                }
            }
        }
        return newBuilder;
    }

    /**
     * Retrieves a list of interceptor instances applicable to a given method, based on a set of interceptor definitions.
     * Each interceptor definition includes a matcher that determines whether the interceptor should be applied to the method.
     *
     * @param methodDesc The description of the method for which interceptors are being retrieved.
     * @param interceptors A list of interceptor definitions to evaluate against the method.
     * @return A list of {@link Interceptor} instances that match the method according to their respective definitions.
     */
    private List<Interceptor> getInterceptors(MethodDescription.InDefinedShape methodDesc, List<InterceptorDefinition> interceptors) {
        List<Interceptor> result = new ArrayList<>(interceptors.size());
        BuddyMethodDesc desc = new BuddyMethodDesc(methodDesc);
        for (InterceptorDefinition interceptor : interceptors) {
            if (!interceptor.getMatcher().match(desc)) {
                continue;
            }
            result.add(interceptor.getInterceptor());
        }
        return result;
    }

    /**
     * Enhances a method by applying a list of interceptors to it. This is achieved by using bytecode manipulation
     * to insert advice around the method execution. The advice is defined in a specified class and is selected
     * based on a unique key.
     *
     * @param builder The builder used to create or modify the class that contains the method.
     * @param methodDesc The description of the method to be enhanced.
     * @param classLoader The class loader of the class being modified.
     * @param interceptors A list of interceptors to apply to the method.
     * @param templateCls The class that contains the advice to be applied to the method.
     * @param adviceKey A unique key identifying the specific advice to use.
     * @return A {@link DynamicType.Builder} instance representing the modified class.
     * @throws InvocationTargetException if an exception occurs while invoking the advice method.
     * @throws IllegalAccessException if the advice method or field is not accessible.
     * @throws NoSuchMethodException if the advice method does not exist.
     * @throws NoSuchFieldException if a required field by the advice does not exist.
     */
    protected DynamicType.Builder<?> enhanceMethod(DynamicType.Builder<?> builder,
                                                   MethodDescription.InDefinedShape methodDesc,
                                                   ClassLoader classLoader,
                                                   List<Interceptor> interceptors,
                                                   Class<?> templateCls,
                                                   String adviceKey)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, NoSuchFieldException {
        AdviceDesc adviceDesc = AdviceHandler.getOrCreate(adviceKey);
        for (Interceptor interceptor : interceptors) {
            adviceDesc.add(interceptor);
        }
        if (adviceDesc.lock(plugin)) {
            return builder.visit(Advice.to(templateCls).on(ElementMatchers.is(methodDesc)));
        }
        return builder;
    }
}
