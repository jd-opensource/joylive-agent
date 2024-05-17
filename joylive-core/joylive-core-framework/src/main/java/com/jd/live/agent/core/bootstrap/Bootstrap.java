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
package com.jd.live.agent.core.bootstrap;

import com.jd.live.agent.bootstrap.classloader.CandidatorOption;
import com.jd.live.agent.bootstrap.classloader.LiveClassLoader;
import com.jd.live.agent.bootstrap.classloader.Resourcer;
import com.jd.live.agent.bootstrap.exception.InitializeException;
import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerBridge;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.util.option.ValueResolver;
import com.jd.live.agent.core.bytekit.ByteSupplier;
import com.jd.live.agent.core.classloader.ClassLoaderManager;
import com.jd.live.agent.core.command.Command;
import com.jd.live.agent.core.config.AgentConfig;
import com.jd.live.agent.core.config.ClassLoaderConfig;
import com.jd.live.agent.core.config.EnhanceConfig;
import com.jd.live.agent.core.config.PluginConfig;
import com.jd.live.agent.core.context.AgentContext;
import com.jd.live.agent.core.context.AgentPath;
import com.jd.live.agent.core.event.*;
import com.jd.live.agent.core.exception.ParseException;
import com.jd.live.agent.core.extension.ExtensibleDesc;
import com.jd.live.agent.core.extension.ExtensionEvent.EventType;
import com.jd.live.agent.core.extension.ExtensionManager;
import com.jd.live.agent.core.extension.condition.ConditionManager;
import com.jd.live.agent.core.extension.condition.ConditionMatcher;
import com.jd.live.agent.core.extension.jplug.JExtensionManager;
import com.jd.live.agent.core.inject.InjectSource;
import com.jd.live.agent.core.inject.InjectSourceSupplier;
import com.jd.live.agent.core.inject.Injection;
import com.jd.live.agent.core.inject.Injection.Injector;
import com.jd.live.agent.core.inject.InjectorFactory;
import com.jd.live.agent.core.inject.annotation.Configurable;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.AppService;
import com.jd.live.agent.core.instance.AppStatus;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.parser.ConfigParser;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.plugin.PluginManager;
import com.jd.live.agent.core.plugin.PluginSupervisor;
import com.jd.live.agent.core.service.AgentService;
import com.jd.live.agent.core.service.ServiceManager;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.network.Ipv4;
import com.jd.live.agent.core.util.option.CascadeOption;
import com.jd.live.agent.core.util.option.MapOption;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.core.util.shutdown.Shutdown;
import com.jd.live.agent.core.util.shutdown.ShutdownHookAdapter;
import com.jd.live.agent.core.util.time.TimeScheduler;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.core.util.type.Artifact;
import com.jd.live.agent.core.util.version.JVM;
import com.jd.live.agent.core.util.version.VersionExpression;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.extension.condition.ConditionMatcher.DEPEND_ON_LOADER;
import static com.jd.live.agent.core.util.type.ClassUtils.describe;

/**
 * Bootstrap is the main entry point for the agent's lifecycle management.
 *
 */
public class Bootstrap implements AgentLifecycle {
    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    /**
     * Instrumentation object provided by the JVM for class instrumentation.
     */
    private final Instrumentation instrumentation;

    /**
     * Indicates whether the agent should operate in a dynamic mode.
     */
    private final boolean dynamic;

    /**
     * Environment variables passed to the agent.
     */
    private final Map<String, Object> env;

    /**
     * Configuration properties passed to the agent.
     */
    private final Map<String, Object> config;

    /**
     * Runnable that handles the unloading of the agent.
     */
    private final Runnable unLoader;

    /**
     * ClassLoader associated with the Bootstrap.
     */
    private final ClassLoader classLoader;

    /**
     * Resolver for interpolating configuration values.
     */
    private final ValueResolver valueResolver;

    /**
     * Path information for the agent.
     */
    private AgentPath agentPath;

    /**
     * Dependency injector for the agent.
     */
    private Injector injector;

    /**
     * Configuration options for the agent.
     */
    private Option option;

    /**
     * Configuration for the agent itself.
     */
    private AgentConfig agentConfig;

    /**
     * The application being instrumented by the agent.
     */
    private Application application;

    /**
     * Configuration for class loading.
     */
    private ClassLoaderConfig classLoaderConfig;

    /**
     * Manages class loaders for isolated plugin execution.
     */
    private ClassLoaderManager classLoaderManager;

    /**
     * Event bus for publishing and subscribing to events within the agent.
     */
    private EventBus eventBus;

    /**
     * Publisher for agent events.
     */
    private Publisher<AgentEvent> publisher;

    private TimeScheduler timer;

    /**
     * Manages services within the agent.
     */
    private ServiceManager serviceManager;

    /**
     * Supervises plugins, handling their lifecycle.
     */
    private PluginSupervisor pluginManager;

    /**
     * Matches conditions for enabling or disabling certain agent features.
     */
    private ConditionMatcher conditionMatcher;

    /**
     * Manages extensions to the agent.
     */
    private ExtensionManager extensionManager;

    /**
     * Context for the agent providing access to its components.
     */
    private AgentContext context;

    /**
     * Supplies bytecode for instrumentation.
     */
    private ByteSupplier byteSupplier;

    /**
     * Bridges logging from the agent to the application's logging system.
     */
    private LoggerBridge loggerBridge;

    /**
     * Manages commands that can be executed within the agent.
     */
    private ExtensibleDesc<Command> commandManager;

    /**
     * Suppliers for injecting sources into the agent's context.
     */
    private List<InjectSourceSupplier> sourceSuppliers;

    private Shutdown shutdown;

    /**
     * Constructs a new Bootstrap instance.
     *
     * @param instrumentation the instrumentation provided by the JVM
     * @param dynamic         whether the agent should operate in dynamic mode
     * @param env             the environment variables
     * @param config          the configuration properties
     * @param unLoader        the runnable to unload the agent
     */
    public Bootstrap(Instrumentation instrumentation, boolean dynamic,
                     Map<String, Object> env, Map<String, Object> config,
                     Runnable unLoader) {
        this.instrumentation = instrumentation;
        this.dynamic = dynamic;
        this.env = env;
        this.config = config;
        this.unLoader = unLoader;
        this.classLoader = Bootstrap.class.getClassLoader();
        this.valueResolver = new ValueResolver(new MapOption(env));
    }

    @Override
    public void install() {
        try {
            agentPath = createAgentPath();
            conditionMatcher = createConditionMatcher();
            extensionManager = createExtensionManager(); // depend on conditionMatcher
            injector = createInjector(); //depend on classloader
            inject();
            option = new MapOption(config); // option include bootstrap.properties.
            classLoaderConfig = createClassLoaderConfig(); //depend on env option
            classLoaderManager = createClassLoaderManager(); //depend on agentPath and env option
            supplyEnv(); //depend on classLoaderManager
            application = createApplication(); //depend on env option
            setupLogger(); //depend on extensionManager
            option = loadConfig(); // load config.yaml and merge bootstrap.properties.
            agentConfig = createAgentConfig(); //depend on option & injector
            context = createAgentContext(); //depend on option & agentPath & agentConfig & application
            timer = new TimeScheduler("LiveTimer", 200, 300, 4, 10);
            timer.start();
            eventBus = createEventBus(); //depend on extensionManager & option
            publisher = eventBus.getPublisher(Publisher.SYSTEM);
            publisher.addHandler(this::onAgentEvent);
            if (!supportEnhance()) {
                //depend on agentConfig
                throw new InitializeException("the jvm version is not supported enhancement.");
            }
            sourceSuppliers = createSourceSuppliers();  //
            serviceManager = createServiceManager(); //depend on extensionManager & classLoaderManager & eventBus & sourceSuppliers
            byteSupplier = createByteSupplier();
            pluginManager = createPluginManager(); //depend on context & extensionManager & classLoaderManager & byteSupplier
            commandManager = createCommandManager();
            subscribe();
            serviceManager.start().join();
            if (pluginManager.install(dynamic)) {
                publisher.offer(new Event<>(new AgentEvent(AgentEvent.EventType.AGENT_ENHANCE_SUCCESS, "success installing all plugins.")));
            } else {
                publisher.offer(new Event<>(new AgentEvent(AgentEvent.EventType.AGENT_ENHANCE_FAILURE, "failed to install plugin.")));
            }
            shutdown = new Shutdown();
            shutdown.addHook(new ShutdownHookAdapter(() -> application.setStatus(AppStatus.DESTROYING), 0));
            shutdown.addHook(() -> serviceManager.stop());
            shutdown.register();
        } catch (Throwable e) {
            publisher.offer(new Event<>(new AgentEvent(AgentEvent.EventType.AGENT_START_FAILURE,
                    e instanceof InitializeException
                            ? e.getMessage()
                            : "failed to install plugin. caused by " + e.getMessage())));
            logger.error(e.getMessage(), e);
            if (serviceManager != null) {
                serviceManager.stop();
            }
        }
    }

    @Override
    public void execute(String command, Map<String, Object> args) {
        if (commandManager == null) {
            //only throw initialize exception without error stack to agent.
            throw new InitializeException("agent is not successfully installed");
        } else if (command == null || command.isEmpty()) {
            //only throw initialize exception without error stack to agent.
            throw new InitializeException("command is empty.");
        } else {
            Command commander = commandManager.getExtension(command);
            if (commander != null) {
                commander.execute(args);
            } else {
                //only throw initialize exception without error stack to agent.
                throw new InitializeException("command " + command + " is not found.");
            }
        }
    }

    @Override
    public void uninstall() {
        // uninstall agent
        if (!dynamic) {
            logger.warn("when using the premain mode, uninstallation is not allowed.");
            return;
        }
        Close.instance()
                .closeIfExists(shutdown, Shutdown::unregister)
                .closeIfExists(timer, TimeScheduler::close)
                .closeIfExists(pluginManager, PluginSupervisor::uninstall)
                .closeIfExists(serviceManager, ServiceManager::close)
                .closeIfExists(eventBus, EventBus::stop)
                .closeIfExists(classLoaderManager, ClassLoaderManager::close)
                .closeIfExists(unLoader, Runnable::run)
                .close((Runnable) LoggerFactory::reset);
        shutdown = null;
        pluginManager = null;
        agentPath = null;
        injector = null;
        option = null;
        agentConfig = null;
        application = null;
        classLoaderConfig = null;
        classLoaderManager = null;
        eventBus = null;
        publisher = null;
        serviceManager = null;
        conditionMatcher = null;
        context = null;
        byteSupplier = null;
        loggerBridge = null;
        commandManager = null;
        sourceSuppliers = null;
        extensionManager = null;
    }

    /**
     * Creates an {@link AgentPath} instance using environment variables.
     * This includes paths for the agent's root, log, and output directories.
     *
     * @return An instance of {@link AgentPath} constructed from environment variables.
     */
    private AgentPath createAgentPath() {
        // Retrieve the root, log, and output paths from the environment variables
        String root = (String) env.get(AgentPath.KEY_AGENT_PATH);
        String log = (String) env.get(AgentPath.KEY_AGENT_LOG_PATH);
        String output = (String) env.get(AgentPath.KEY_AGENT_OUTPUT_PATH);

        // Create and return an AgentPath instance with the retrieved paths
        return new AgentPath(
                new File(root), // Root path
                log != null && !log.isEmpty() ? new File(log) : null, // Log path, null if not specified or empty
                output != null && !output.isEmpty() ? new File(output) : null); // Output path, null if not specified or empty
    }

    /**
     * Loads the configuration from a file and merges it with the bootstrap properties.
     * The configuration file is expected to be in YAML format.
     *
     * @return An {@link Option} instance containing the merged configuration.
     */
    private Option loadConfig() {
        // Retrieve the configuration file from the agent path
        File file = agentPath.getConfigFile();
        CascadeOption result;
        try {
            // Load the YAML configuration from the file
            result = new CascadeOption(loadConfig(new FileReader(file), ObjectParser.YAML));
            // Merge the bootstrap properties into the loaded configuration
            config.forEach(result::put);
            return result;
        } catch (LiveException e) {
            throw e;
        } catch (Throwable e) {
            throw new ParseException("failed to parse file " + file.getPath(), e);
        }
    }

    /**
     * Loads the configuration from a given {@link Reader} source and parses it using the specified type.
     *
     * @param src  The {@link Reader} source containing the configuration data.
     * @param type The type of the parser to use (e.g., YAML, JSON).
     * @return A {@link Map} representing the parsed configuration.
     * @throws Exception if there is an error in reading or parsing the configuration.
     */
    private Map<String, Object> loadConfig(Reader src, String type) throws Exception {
        try (Reader reader = new BufferedReader(src)) {
            ConfigParser parser = extensionManager.getOrLoadExtension(ConfigParser.class, type, classLoaderManager.getCoreImplLoader());
            return parser.parse(reader);
        }
    }

    /**
     * Creates a {@link ClassLoaderConfig} instance, fills its properties through dependency injection,
     * and sets the context loader enable state in {@link CandidatorOption}.
     *
     * @return A configured {@link ClassLoaderConfig} instance.
     */
    private ClassLoaderConfig createClassLoaderConfig() {
        ClassLoaderConfig config = new ClassLoaderConfig();
        injector.inject(config);
        CandidatorOption.setContextLoaderEnabled(config.isContextLoaderEnabled());
        return config;
    }

    private void supplyEnv() {
        List<EnvSupplier> suppliers = extensionManager.getOrLoadExtensible(EnvSupplier.class,
                classLoaderManager.getCoreImplLoader()).getExtensions();
        suppliers.forEach(supplier -> supplier.process(env));
    }

    private Application createApplication() {
        Application app = new Application();
        injector.inject(app);
        AppService appService = app.getService();
        Location location = app.getLocation();
        if (location == null) {
            location = new Location();
            app.setLocation(location);
        }
        if (appService == null) {
            appService = new AppService();
            app.setService(appService);
        }
        location.setIp(Ipv4.getLocalIp());
        location.setHost(Ipv4.getLocalHost());
        setProperty(Application.KEY_INSTANCE_ID, app.getInstance());
        setProperty(Application.KEY_SERVICE, appService == null ? null : appService.getName());
        setProperty(Application.KEY_LIVE_SPACE_ID, location.getLiveSpaceId() == null ? null : String.valueOf(location.getLiveSpaceId()));
        setProperty(Application.KEY_UNIT, location.getUnit());
        setProperty(Application.KEY_CELL, location.getCell());
        setProperty(Application.KEY_LANE_SPACE_ID, location.getLaneSpaceId() == null ? null : String.valueOf(location.getLaneSpaceId()));
        setProperty(Application.KEY_LANE_CODE, location.getLane());
        return app;
    }

    private void setProperty(String key, String value) {
        if (value != null) {
            System.setProperty(key, value);
        }
    }

    private AgentConfig createAgentConfig() {
        AgentConfig result = new AgentConfig();
        injector.inject(result);
        return result;
    }

    private AgentContext createAgentContext() {
        Artifact artifact = describe(Bootstrap.class).getArtifact();
        return new AgentContext(instrumentation, dynamic, classLoader, agentPath, option,
                agentConfig, artifact.getVersion(), application);
    }

    private Injector createInjector() {
        InjectorFactory factory = extensionManager.getOrLoadExtension(InjectorFactory.class);
        final Injection injection = factory.create(extensionManager, new MapOption(env), classLoader);

        return target -> {
            Injectable injectable = target.getClass().getAnnotation(Injectable.class);
            Configurable configurable = target.getClass().getAnnotation(Configurable.class);
            if (injectable != null && injectable.enable() || configurable != null) {
                Map<String, Object> components = new HashMap<>();
                InjectSource ctx = new InjectSource(option, components);
                ctx.add(AgentConfig.COMPONENT_AGENT_CONFIG, agentConfig);
                ctx.add(EnhanceConfig.COMPONENT_ENHANCE_CONFIG, agentConfig == null ? null : agentConfig.getEnhanceConfig());
                ctx.add(PluginConfig.COMPONENT_PLUGIN_CONFIG, agentConfig == null ? null : agentConfig.getPluginConfig());
                ctx.add(AgentPath.COMPONENT_AGENT_PATH, agentPath);
                ctx.add(Application.COMPONENT_APPLICATION, application);
                ctx.add(ExtensionManager.COMPONENT_EXTENSION_MANAGER, extensionManager);
                ctx.add(Timer.COMPONENT_TIMER, timer);
                ctx.add(EventBus.COMPONENT_EVENT_BUS, eventBus);
                ctx.add(Resourcer.COMPONENT_RESOURCER, classLoaderManager == null ? null : classLoaderManager.getPluginLoaders());
                ctx.add(Resourcer.COMPONENT_CLASSLOADER_CORE, classLoader);
                ctx.add(Resourcer.COMPONENT_CLASSLOADER_CORE_IMPL, classLoaderManager == null ? null : classLoaderManager.getCoreImplLoader());
                ctx.add(ClassLoaderConfig.COMPONENT_CLASSLOADER_CONFIG, classLoaderConfig);
                ctx.add(AgentLifecycle.COMPONENT_AGENT_LIFECYCLE, Bootstrap.this);
                ctx.add(ConditionMatcher.COMPONENT_CONDITION_MATCHER, conditionMatcher);
                if (sourceSuppliers != null) {
                    sourceSuppliers.forEach(s -> s.apply(ctx));
                }
                injection.inject(ctx, target);
            }
        };
    }

    private ServiceManager createServiceManager() {
        List<AgentService> services = extensionManager.getOrLoadExtensible(AgentService.class,
                classLoaderManager.getCoreImplLoader()).getExtensions();
        return new ServiceManager(services, publisher);
    }

    private PluginSupervisor createPluginManager() {
        return new PluginManager(context, extensionManager, classLoaderManager.getPluginLoaders(), byteSupplier);
    }

    private ClassLoaderManager createClassLoaderManager() {
        return new ClassLoaderManager((LiveClassLoader) classLoader, classLoaderConfig, agentPath);
    }

    private ConditionMatcher createConditionMatcher() {
        return new ConditionManager(classLoader,
                key -> option == null ? null : (String) valueResolver.parse(option.getString(key)),
                DEPEND_ON_LOADER.negate());
    }

    private ExtensionManager createExtensionManager() {
        return new JExtensionManager(conditionMatcher);
    }

    private EventBus createEventBus() {
        EventBus bus = extensionManager.getOrLoadExtension(EventBus.class, classLoaderManager.getCoreImplLoader());
        injector.inject(bus);
        return bus;
    }

    private ByteSupplier createByteSupplier() {
        return extensionManager.getOrLoadExtension(ByteSupplier.class, classLoaderManager.getCoreImplLoader());
    }

    private List<InjectSourceSupplier> createSourceSuppliers() {
        return extensionManager.getOrLoadExtensible(InjectSourceSupplier.class, classLoaderManager.getCoreImplLoader()).getExtensions();
    }

    private void setupLogger() {
        loggerBridge = extensionManager.getOrLoadExtension(LoggerBridge.class, classLoaderManager.getCoreImplLoader());
        if (loggerBridge != null) {
            LoggerFactory.setBridge(loggerBridge);
        }
        Location location = application.getLocation();
        logger.info(String.format("Starting application name=%s, instance=%s, location=[region=%s,zone=%s,unit=%s,cell=%s,lane=%s]",
                application.getName(), application.getInstance(), location.getRegion(), location.getZone(),
                location.getUnit(), location.getCell(), location.getLane()));
    }

    private boolean supportEnhance() {
        String version = agentConfig.getEnhanceConfig().getJavaVersion();
        version = version == null || version.isEmpty() ? EnhanceConfig.SUPPORT_JAVA_VERSION : version;
        return VersionExpression.of(version).match(JVM.instance().getVersion());
    }

    private void inject() {
        extensionManager.addListener((event -> {
            if (event.getType() == EventType.CREATED) {
                injector.inject(event.getInstance());
            } else {
                Throwable throwable = event.getThrowable();
                logger.error(throwable.getMessage(), throwable);
            }
        }));
    }

    private void subscribe() {
        List<Subscription> subscriptions = extensionManager.getOrLoadExtensible(Subscription.class,
                classLoaderManager.getCoreImplLoader()).getExtensions();
        for (Subscription subscription : subscriptions) {
            eventBus.getPublisher(subscription.getTopic()).addHandler(subscription);
        }
    }

    private ExtensibleDesc<Command> createCommandManager() {
        return extensionManager.getOrLoadExtensible(Command.class, classLoaderManager.getCoreImplLoader());
    }

    private void onAgentEvent(List<Event<AgentEvent>> events) {
        for (Event<AgentEvent> event : events) {
            AgentEvent data = event.getData();
            switch (data.getType()) {
                case AGENT_START_FAILURE:
                case AGENT_POLICY_INITIALIZE_FAILURE:
                    logger.error(data.getMessage(), data.getThrowable());
                    logger.info("shutdown.....");
                    System.exit(1);
                case AGENT_ENHANCE_FAILURE:
                    logger.error(data.getMessage(), data.getThrowable());
                    if (!dynamic) {
                        logger.info("shutdown.....");
                        System.exit(1);
                    }
            }
        }
    }
}
