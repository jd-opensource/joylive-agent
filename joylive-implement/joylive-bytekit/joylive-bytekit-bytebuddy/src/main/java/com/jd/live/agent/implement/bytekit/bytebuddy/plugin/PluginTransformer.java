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
import com.jd.live.agent.implement.bytekit.bytebuddy.advice.ConstructorAdvice;
import com.jd.live.agent.implement.bytekit.bytebuddy.advice.MemberMethodAdvice;
import com.jd.live.agent.implement.bytekit.bytebuddy.advice.StaticMethodAdvice;
import com.jd.live.agent.implement.bytekit.bytebuddy.type.BuddyMethodDesc;
import com.jd.live.agent.implement.bytekit.bytebuddy.type.BuddyTypeDesc;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.reflect.InvocationTargetException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.core.extension.condition.ConditionMatcher.DEPEND_ON_LOADER;

/**
 * PluginTransformer
 *
 * @since 1.0.0
 */
public class PluginTransformer implements AgentBuilder.RawMatcher, AgentBuilder.Transformer {

    private static final Logger logger = LoggerFactory.getLogger(PluginTransformer.class);

    private final Map<String, Boolean> definitionEnabled = new ConcurrentHashMap<>();

    private final PluginDeclare plugin;

    private final ConditionMatcher conditionMatcher;

    private final Map<String, List<InterceptorDefinition>> types = new ConcurrentHashMap<>();

    public PluginTransformer(PluginDeclare plugin, ConditionMatcher conditionMatcher) {
        this.plugin = plugin;
        this.conditionMatcher = conditionMatcher;
    }

    @Override
    public boolean matches(TypeDescription description, ClassLoader loader, JavaModule module, Class<?> type, ProtectionDomain domain) {
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
            Class<?> definitionClass = definition.getClass();
            // match the type
            if (definition.getMatcher().match(typeDesc)) {
                // determine whether the plugin is enabled in this classloader.
                if (definitionEnabled.computeIfAbsent(definitionClass.getName() + (loader == null ? "" : ("@" + loader)),
                        n -> conditionMatcher.match(definitionClass, loader, DEPEND_ON_LOADER))) {
                    Collections.addAll(matched, definition.getInterceptors());
                }
            }
        }
        return !matched.isEmpty() && types.putIfAbsent(uniqueName, matched) == null;
    }

    private String getUniqueName(TypeDescription description, ClassLoader loader) {
        return description.getActualName() + (loader == null ? "" : ("@" + loader));
    }

    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                            TypeDescription description,
                                            ClassLoader loader,
                                            JavaModule module,
                                            ProtectionDomain domain) {
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
