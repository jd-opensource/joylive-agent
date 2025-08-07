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
package com.jd.live.agent.implement.bytekit.bytebuddy;

import com.jd.live.agent.bootstrap.classloader.LiveClassLoader;
import com.jd.live.agent.bootstrap.classloader.ResourceConfig;
import com.jd.live.agent.bootstrap.classloader.Resourcer;
import com.jd.live.agent.bootstrap.classloader.ResourcerType;
import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.bytekit.ByteBuilder;
import com.jd.live.agent.core.bytekit.ByteSupplier;
import com.jd.live.agent.core.classloader.ClassLoaderManager;
import com.jd.live.agent.core.config.*;
import com.jd.live.agent.core.event.EventBus;
import com.jd.live.agent.core.exception.ParseException;
import com.jd.live.agent.core.extension.ExtensibleDesc;
import com.jd.live.agent.core.extension.ExtensionEvent;
import com.jd.live.agent.core.extension.ExtensionManager;
import com.jd.live.agent.core.extension.condition.ConditionManager;
import com.jd.live.agent.core.extension.condition.ConditionMatcher;
import com.jd.live.agent.core.extension.jplug.JExtensionManager;
import com.jd.live.agent.core.inject.InjectSource;
import com.jd.live.agent.core.inject.Injection;
import com.jd.live.agent.core.inject.annotation.Configurable;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.inject.jbind.*;
import com.jd.live.agent.core.inject.jbind.converter.BestSelector;
import com.jd.live.agent.core.inject.jbind.supplier.JInjectionContext;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ConfigParser;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.plugin.Plugin;
import com.jd.live.agent.core.plugin.PluginType;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.util.option.CascadeOption;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.governance.policy.PolicyManager;
import com.jd.live.agent.governance.policy.PolicySupervisor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.implement.bytekit.bytebuddy.test.Foo;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * InterceptorTest
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class InterceptorTest {
    private static final Logger logger = LoggerFactory.getLogger(InterceptorTest.class);
    private static ExtensionManager extensionManager;
    private static Instrumentation instrumentation;
    private static ClassLoader classLoader;
    private static EventBus bus;
    private static Application app;
    private static AgentConfig agentConfig;
    private static ClassLoaderConfig classLoaderConfig;
    private static AgentPath agentPath;
    private static Map<String, Object> env;
    private static ConditionMatcher conditionMatcher;
    private static LiveClassLoader coreClassLoader;
    private static PolicyManager policyManager;
    private static ClassLoaderManager classLoaderManager;
    private static Option option;
    private static Injection.Injector injector;
    private static URL[] urls;

    @BeforeAll
    public static void initialize() {
        instrumentation = ByteBuddyAgent.install();
        extensionManager = new JExtensionManager();
        classLoader = InterceptorTest.class.getClassLoader();
        bus = extensionManager.getOrLoadExtension(EventBus.class, classLoader);
        env = new HashMap<>();
        env.put(AgentPath.KEY_AGENT_PATH, InterceptorTest.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        agentConfig = new AgentConfig();
        agentPath = new AgentPath(new File((String) env.get(AgentPath.KEY_AGENT_PATH)));
        classLoaderConfig = new ClassLoaderConfig();
        conditionMatcher = new ConditionManager(classLoader, null, null);
        urls = agentPath.getLibUrls(agentPath.getRoot().getParentFile());
        ResourceConfig config = new ResourceConfig();
        coreClassLoader = new LiveClassLoader(urls, InterceptorTest.class.getClassLoader(), ResourcerType.CORE,
                config, classLoaderConfig.getEssentialResource().getBootstrap(), agentPath.getConfigPath(), "test");
        injector = createInjector(coreClassLoader);
        extensionManager.addListener((event -> {
            if (event.getType() == ExtensionEvent.EventType.CREATED) {
                injector.inject(event.getInstance());
            } else {
                Throwable throwable = event.getThrowable();
                logger.error(throwable.getMessage(), throwable);
            }
        }));
        app = new Application();
        policyManager = new PolicyManager();
        option = createOption(agentPath);
        classLoaderManager = new ClassLoaderManager(coreClassLoader, classLoaderConfig, agentPath);
        injector.inject(bus);
        injector.inject(app);
        injector.inject(agentConfig);
        injector.inject(classLoaderConfig);
    }

    private static Option createOption(AgentPath agentPath) {
        File file = agentPath.getConfigFile();
        try (Reader reader = new BufferedReader(new FileReader(file))) {
            ConfigParser parser = extensionManager.getOrLoadExtension(ConfigParser.class, ObjectParser.YAML, classLoader);
            CascadeOption mapOption = new CascadeOption(parser.parse(reader));
            for (Map.Entry<String, Object> entry : env.entrySet()) {
                mapOption.put(entry.getKey(), entry.getValue());
            }
            mapOption.put(Application.CONFIG_APP_INSTANCE, UUID.randomUUID().toString());
            return mapOption;
        } catch (LiveException e) {
            throw e;
        } catch (Throwable e) {
            throw new ParseException("failed to parse file " + file.getPath(), e);
        }
    }

    private static Injection.Injector createInjector(LiveClassLoader classLoader) {
        List<InjectionSupplier> suppliers = extensionManager.getOrLoadExtensible(InjectionSupplier.class, classLoader).getExtensions();
        List<ConverterSupplier> converterSuppliers = extensionManager.getOrLoadExtensible(ConverterSupplier.class, classLoader).getExtensions();
        List<Converter.FundamentalConverter> fundamentalConverters = extensionManager.getOrLoadExtensible(Converter.FundamentalConverter.class, classLoader).getExtensions();
        List<ArrayBuilder> arrayBuilders = extensionManager.getOrLoadExtensible(ArrayBuilder.class, classLoader).getExtensions();


        BestSelector bestSelector = new BestSelector(converterSuppliers, fundamentalConverters, arrayBuilders);
        final Injectors injectors = new Injectors(suppliers, new JInjectionContext(bestSelector, bestSelector, option));

        return target -> {
            Injectable injectable = target.getClass().getAnnotation(Injectable.class);
            Configurable configurable = target.getClass().getAnnotation(Configurable.class);
            if (injectable != null && injectable.enable() || configurable != null) {
                Map<String, Object> components = new HashMap<>();
                components.put(AgentConfig.COMPONENT_AGENT_CONFIG, agentConfig);
                components.put(EnhanceConfig.COMPONENT_ENHANCE_CONFIG, agentConfig.getEnhanceConfig());
                components.put(PluginConfig.COMPONENT_PLUGIN_CONFIG, agentConfig.getPluginConfig());
                components.put(AgentPath.COMPONENT_AGENT_PATH, agentPath);
                components.put(Application.COMPONENT_APPLICATION, app);
                components.put(PolicySupervisor.COMPONENT_POLICY_SUPERVISOR, policyManager);
                components.put(PolicySupplier.COMPONENT_POLICY_SUPPLIER, policyManager);
                components.put(ExtensionManager.COMPONENT_EXTENSION_MANAGER, extensionManager);
                components.put(EventBus.COMPONENT_EVENT_BUS, bus);
                components.put(ConditionMatcher.COMPONENT_CONDITION_MATCHER, conditionMatcher);
                components.put(Resourcer.COMPONENT_RESOURCER, classLoaderManager.getPluginLoaders());
                InjectSource ctx = new InjectSource(option, components);
                injectors.inject(ctx, target);
            }
        };
    }

    @Test
    public void testEnhanceClass() {
        // instrumentation.appendToSystemClassLoaderSearch(new JarFile(new File(agentPath.getRoot().getParent() + "/joylive-test-bytebuddy-1.0.0.jar")));
        // + "/joylive-test-bytebuddy-1.0.0.jar"
        Plugin plugin = Plugin.builder()
                .path(new File(agentPath.getRoot().getParent()))
                .type(PluginType.DYNAMIC)
                .urls(urls)
                .loader(extensionManager.build(PluginDefinition.class, coreClassLoader))
                .conditionMatcher(conditionMatcher)
                .build();
        plugin.load();

        ExtensibleDesc<ByteSupplier> byteSupplierExtensibleDesc = extensionManager.getOrLoadExtensible(ByteSupplier.class);
        ByteSupplier byteSupplier = byteSupplierExtensibleDesc.getExtension();
        injector.inject(byteSupplier);
        ByteBuilder byteBuilder = byteSupplier.create();
        // instrumentation
        byteBuilder.append(plugin).install(instrumentation);

        Foo foo = new Foo();
        logger.info(foo.say());
    }
}
