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

import com.jd.live.agent.bootstrap.classloader.LiveClassLoader;
import com.jd.live.agent.bootstrap.classloader.Resourcer;
import com.jd.live.agent.bootstrap.exception.InitializeException;
import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerBridge;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.util.option.ValueResolver;
import com.jd.live.agent.core.bootstrap.AppListener.AppListenerWrapper;
import com.jd.live.agent.core.bytekit.ByteSupplier;
import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.classloader.ClassLoaderManager;
import com.jd.live.agent.core.command.Command;
import com.jd.live.agent.core.config.*;
import com.jd.live.agent.core.event.*;
import com.jd.live.agent.core.event.EventHandler.EventProcessor;
import com.jd.live.agent.core.exception.EnhanceException;
import com.jd.live.agent.core.exception.ParseException;
import com.jd.live.agent.core.extension.ExtensibleDesc;
import com.jd.live.agent.core.extension.ExtensionDesc;
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
import com.jd.live.agent.core.instance.AppStatus;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.parser.ConfigParser;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.plugin.PluginManager;
import com.jd.live.agent.core.plugin.PluginSupervisor;
import com.jd.live.agent.core.security.*;
import com.jd.live.agent.core.security.detector.DefaultCipherDetector;
import com.jd.live.agent.core.service.ServiceManager;
import com.jd.live.agent.core.service.ServiceSupervisor;
import com.jd.live.agent.core.service.ServiceSupervisorAware;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.option.CascadeOption;
import com.jd.live.agent.core.util.option.MapOption;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.core.util.shutdown.Shutdown;
import com.jd.live.agent.core.util.shutdown.ShutdownHookAdapter;
import com.jd.live.agent.core.util.time.TimeScheduler;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.core.util.version.JVM;
import com.jd.live.agent.core.util.version.VersionExpression;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jd.live.agent.core.extension.condition.ConditionMatcher.DEPEND_ON_LOADER;

/**
 * Bootstrap is the main entry point for the agent's lifecycle management.
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
    private final AppEnv env;

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

    private CipherConfig cipherConfig;

    private CipherDetector cipherDetector;

    private CipherFactory cipherFactory;

    private Cipher cipher;

    private StringDecrypter stringDecrypter;

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

    private AppListener applicationListener;

    /**
     * Supervises plugins, handling their lifecycle.
     */
    private PluginSupervisor pluginManager;

    /**
     * Subscription, handling the event.
     */
    private List<Subscriber> subscribers;

    /**
     * Matches conditions for enabling or disabling certain agent features.
     */
    private ConditionMatcher conditionMatcher;

    /**
     * Manages extensions to the agent.
     */
    private JExtensionManager extensionManager;

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

    private final AtomicBoolean installed = new AtomicBoolean(false);

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
        this.env = new AppEnv(env);
        this.config = config;
        this.unLoader = unLoader;
        this.classLoader = Bootstrap.class.getClassLoader();
        this.valueResolver = new ValueResolver(MapOption.of(env));
    }

    @Override
    public void install() {
        try {
            agentPath = createAgentPath();
            conditionMatcher = createConditionMatcher();
            extensionManager = createExtensionManager(); // depend on conditionMatcher
            injector = createInjector(); //depend on classloader
            inject();
            option = MapOption.of(config); // option include bootstrap.properties.
            classLoaderConfig = createClassLoaderConfig(); //depend on env option
            classLoaderManager = createClassLoaderManager(); //depend on agentPath and env option
            supplyEnv(); //depend on classLoaderManager
            application = createApplication(); //depend on env option
            setupLogger(); //depend on extensionManager
            cipherConfig = createCipherConfig();
            cipherDetector = createCipherDetector(cipherConfig);
            cipherFactory = createCipherFactory();
            cipher = createCipher(cipherConfig, cipherFactory);
            stringDecrypter = createStringDecrypter(cipherDetector, cipher);
            option = loadConfig(); // load config.yaml and merge bootstrap.properties.
            agentConfig = createAgentConfig(); //depend on option & injector
            timer = createTimer();
            timer.start();
            eventBus = createEventBus(); //depend on extensionManager & option
            publisher = eventBus.getPublisher(Publisher.SYSTEM);
            publisher.addHandler((EventProcessor<AgentEvent>) this::onAgentEvent);
            if (!supportEnhance()) {
                //depend on agentConfig
                throw new InitializeException("the jvm version is not supported enhancement.");
            }
            createSourceSuppliers();
            serviceManager = createServiceManager(); //depend on extensionManager & classLoaderManager & eventBus & sourceSuppliers
            setupServiceManager(); // inject to source supplier
            applicationListener = new AppListenerWrapper(createApplicationListeners()); // depend on source suppliers & serviceManager
            byteSupplier = createByteSupplier();
            pluginManager = createPluginManager(); //depend on context & extensionManager & classLoaderManager & byteSupplier
            commandManager = createCommandManager();
            subscribers = createSubscribers();
            subscribe();
            printExtensions();
            // TODO In AgentMain mode, it is necessary to enhance the registry first to obtain the service strategy, and then enhance the routing plugin
            if (pluginManager.install(dynamic)) {
                publisher.offer(AgentEvent.onAgentEnhanceReady("Success installing all plugins."));
            } else {
                throw new EnhanceException("Failed to install plugin.");
            }
            installed.set(true);
            shutdown = new Shutdown();
            shutdown.addHook(new ShutdownHookAdapter(() -> application.setStatus(AppStatus.DESTROYING), 0));
            shutdown.addHook(serviceManager);
            shutdown.register();
            publisher.offer(AgentEvent.onAgentReady("Success starting LiveAgent."));
        } catch (Throwable e) {
            // TODO Close resource
            String error = "Agent is not successfully installed, caused by " + e.getMessage();
            if (publisher != null) {
                AgentEvent event = e instanceof EnhanceException
                        ? AgentEvent.onAgentEnhanceFailure(error, e)
                        : AgentEvent.onAgentFailure(error, e);
                publisher.offer(event);
            } else {
                onException(error, e);
            }
        }
    }

    @Override
    public void uninstall() {
        // uninstall agent
        if (!dynamic) {
            logger.warn("When using the premain mode, uninstallation is not allowed.");
            return;
        }
        Close.instance()
                .close(shutdown)
                .close(timer)
                .close(pluginManager)
                .close(serviceManager)
                .close(eventBus)
                .close(classLoaderManager)
                .closeIfExists(unLoader, Runnable::run)
                .close(subscribers)
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
        byteSupplier = null;
        loggerBridge = null;
        commandManager = null;
        sourceSuppliers = null;
        extensionManager = null;
        subscribers = null;
    }

    @Override
    public void execute(String command, Map<String, Object> args) {
        if (commandManager == null) {
            //only throw initialize exception without error stack to agent.
            throw new InitializeException("CommandManager is not initialized.");
        } else if (command == null || command.isEmpty()) {
            //only throw initialize exception without error stack to agent.
            throw new InitializeException("Command is empty.");
        } else {
            Command commander = commandManager.getExtension(command);
            if (commander != null) {
                commander.execute(args);
            } else {
                //only throw initialize exception without error stack to agent.
                throw new InitializeException("Command " + command + " is not found.");
            }
        }
    }

    private void printExtensions() {
        StringBuilder builder = new StringBuilder();
        extensionManager.forEach(extensible -> {
            builder.delete(0, builder.length());
            builder.append(extensible.getName().getClazz().getName()).append(": ");
            Iterable<? extends ExtensionDesc<?>> extensionDescs = extensible.getExtensionDescs();
            int i = 0;
            for (ExtensionDesc<?> extensionDesc : extensionDescs) {
                if (i++ > 0) {
                    builder.append(", ");
                }
                builder.append(extensionDesc.getName().getName());
            }
            logger.info("Load extensions " + builder);
        });

    }

    /**
     * Creates an {@link AgentPath} instance using environment variables.
     * This includes paths for the agent's root, log, and output directories.
     *
     * @return An instance of {@link AgentPath} constructed from environment variables.
     */
    private AgentPath createAgentPath() {
        // Retrieve the root, log, and output paths from the environment variables
        String root = env.getString(AgentPath.KEY_AGENT_PATH);
        String log = env.getString(AgentPath.KEY_AGENT_LOG_PATH);
        String output = env.getString(AgentPath.KEY_AGENT_OUTPUT_PATH);

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
            throw new ParseException("Failed to parse file " + file.getPath(), e);
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
     *
     * @return A configured {@link ClassLoaderConfig} instance.
     */
    private ClassLoaderConfig createClassLoaderConfig() {
        ClassLoaderConfig config = new ClassLoaderConfig();
        injector.inject(config);
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
        app.initialize();
        return app;
    }

    private AgentConfig createAgentConfig() {
        AgentConfig result = new AgentConfig();
        injector.inject(result);
        return result;
    }

    private Injector createInjector() {
        InjectorFactory factory = extensionManager.getOrLoadExtension(InjectorFactory.class);
        final Injection injection = factory.create(extensionManager, MapOption.of(env.getEnvs()), classLoader);

        return target -> {
            Injectable injectable = target.getClass().getAnnotation(Injectable.class);
            Configurable configurable = target.getClass().getAnnotation(Configurable.class);
            if (injectable != null && injectable.enable() || configurable != null) {
                Map<String, Object> components = new HashMap<>();
                InjectSource ctx = new InjectSource(option, components);
                ctx.add(Option.COMPONENT_OPTION, option);
                ctx.add(AppEnv.COMPONENT_APP_ENV, env);
                ctx.add(CipherConfig.COMPONENT_CIPHER_CONFIG, cipherConfig);
                ctx.add(CipherDetector.COMPONENT_CIPHER_DETECTOR, cipherDetector);
                ctx.add(CipherFactory.COMPONENT_CIPHER_FACTORY, cipherFactory);
                ctx.add(Cipher.COMPONENT_CIPHER, cipher);
                ctx.add(StringDecrypter.COMPONENT_STRING_DECRYPTER, stringDecrypter);
                ctx.add(AgentConfig.COMPONENT_AGENT_CONFIG, agentConfig);
                ctx.add(EnhanceConfig.COMPONENT_ENHANCE_CONFIG, agentConfig == null ? null : agentConfig.getEnhanceConfig());
                ctx.add(PluginConfig.COMPONENT_PLUGIN_CONFIG, agentConfig == null ? null : agentConfig.getPluginConfig());
                ctx.add(AgentPath.COMPONENT_AGENT_PATH, agentPath);
                ctx.add(Application.COMPONENT_APPLICATION, application);
                ctx.add(ExtensionManager.COMPONENT_EXTENSION_MANAGER, extensionManager);
                ctx.add(ServiceSupervisor.COMPONENT_SERVICE_SUPERVISOR, serviceManager);
                ctx.add(AppListener.COMPONENT_APPLICATION_LISTENER, applicationListener);
                ctx.add(Timer.COMPONENT_TIMER, timer);
                ctx.add(EventBus.COMPONENT_EVENT_BUS, eventBus);
                ctx.add(Resourcer.COMPONENT_RESOURCER, classLoaderManager == null ? null : classLoaderManager.getPluginLoaders());
                ctx.add(Resourcer.COMPONENT_CLASSLOADER_CORE, classLoader);
                ctx.add(Resourcer.COMPONENT_CLASSLOADER_CORE_IMPL, classLoaderManager == null ? null : classLoaderManager.getCoreImplLoader());
                ctx.add(ClassLoaderConfig.COMPONENT_CLASSLOADER_CONFIG, classLoaderConfig);
                ctx.add(AgentLifecycle.COMPONENT_AGENT_LIFECYCLE, Bootstrap.this);
                ctx.add(ConditionMatcher.COMPONENT_CONDITION_MATCHER, conditionMatcher);
                if (serviceManager != null) {
                    serviceManager.service(service -> {
                        if (service instanceof InjectSourceSupplier) {
                            ((InjectSourceSupplier) service).apply(ctx);
                        }
                    });
                }
                if (sourceSuppliers != null) {
                    sourceSuppliers.forEach(s -> s.apply(ctx));
                }
                injection.inject(ctx, target);
            }
        };
    }

    private ServiceManager createServiceManager() {
        ServiceManager result = new ServiceManager();
        injector.inject(result);
        return result;
    }

    private void setupServiceManager() {
        for (InjectSourceSupplier supplier : sourceSuppliers) {
            if (supplier instanceof ServiceSupervisorAware) {
                ((ServiceSupervisorAware) supplier).setup(serviceManager);
            }
        }
    }

    private List<AppListener> createApplicationListeners() {
        List<AppListener> extensions = extensionManager.getOrLoadExtensible(AppListener.class, classLoaderManager.getCoreImplLoader()).getExtensions();
        List<AppListener> result = new ArrayList<>(extensions);
        serviceManager.service(service -> {
            if (service instanceof AppListenerSupplier) {
                result.add(((AppListenerSupplier) service).getAppListener());
            }
        });
        return result;
    }

    private PluginSupervisor createPluginManager() {
        return new PluginManager(instrumentation, agentConfig.getPluginConfig(), agentPath, extensionManager,
                classLoaderManager.getPluginLoaders(), byteSupplier, conditionMatcher);
    }

    private ClassLoaderManager createClassLoaderManager() {
        return new ClassLoaderManager((LiveClassLoader) classLoader, classLoaderConfig, agentPath);
    }

    private CipherConfig createCipherConfig() {
        CipherConfig config = new CipherConfig();
        injector.inject(config);
        return config;
    }

    private CipherDetector createCipherDetector(CipherConfig config) {
        // TODO how to decrypt config password;
        return new DefaultCipherDetector(config);
    }

    private CipherFactory createCipherFactory() {
        Map<String, CipherAlgorithmFactory> cipherAlgorithmFactories = extensionManager.getOrLoadExtensible(CipherAlgorithmFactory.class).getExtensionMap();
        Map<String, StringCodec> stringCodecs = extensionManager.getOrLoadExtensible(StringCodec.class).getExtensionMap();
        Map<String, CipherGeneratorFactory> saltFactories = extensionManager.getOrLoadExtensible(CipherGeneratorFactory.class).getExtensionMap();
        return new DefaultCipherFactory(cipherAlgorithmFactories, stringCodecs, saltFactories);
    }

    private Cipher createCipher(CipherConfig config, CipherFactory factory) {
        return factory.create(config);
    }

    private StringDecrypter createStringDecrypter(CipherDetector detector, Cipher cipher) {
        return new DefaultStringDecrypter(detector, cipher);
    }

    private ConditionMatcher createConditionMatcher() {
        return new ConditionManager(classLoader,
                key -> option == null ? null : (String) valueResolver.parse(option.getString(key)),
                DEPEND_ON_LOADER.negate());
    }

    private JExtensionManager createExtensionManager() {
        return new JExtensionManager(conditionMatcher);
    }

    private TimeScheduler createTimer() {
        TimerConfig config = agentConfig.getTimerConfig();
        return new TimeScheduler("LiveAgent-timer", config.getTickTime(), config.getTicks(), config.getWorkerThreads(), config.getMaxTasks());
    }

    private EventBus createEventBus() {
        EventBus bus = extensionManager.getOrLoadExtension(EventBus.class, classLoaderManager.getCoreImplLoader());
        injector.inject(bus);
        return bus;
    }

    private ByteSupplier createByteSupplier() {
        MatcherBuilder.exclusion = agentConfig.getEnhanceConfig()::isExclude;
        ByteSupplier result = extensionManager.getOrLoadExtension(ByteSupplier.class, classLoaderManager.getCoreImplLoader());
        Map<String, Set<String>> addOpens = agentConfig.getEnhanceConfig().getAddOpens();
        if (addOpens != null) {
            result.export(instrumentation, addOpens, classLoaderManager.getCoreImplLoader());
        }
        // add system environments after add opens
        env.addSystem();
        return result;
    }

    private void createSourceSuppliers() {
        sourceSuppliers = new ArrayList<>();
        Injector backup = injector;
        injector = target -> {
            if (target instanceof InjectSourceSupplier) {
                // add source supplier first for injection
                sourceSuppliers.add((InjectSourceSupplier) target);
            }
            backup.inject(target);
        };
        extensionManager.getOrLoadExtensible(InjectSourceSupplier.class, classLoaderManager.getCoreImplLoader()).getExtensions();
        injector = backup;
    }

    private void setupLogger() {
        loggerBridge = extensionManager.getOrLoadExtension(LoggerBridge.class, classLoaderManager.getCoreImplLoader());
        if (loggerBridge != null) {
            LoggerFactory.setBridge(loggerBridge);
        }
        Location location = application.getLocation();
        logger.info("Starting application name={}, instance={}, location=[{}]",
                application.getName(), application.getInstance(), location.toString(null, null));
    }

    private boolean supportEnhance() {
        String version = agentConfig.getEnhanceConfig().getJavaVersion();
        version = version == null || version.isEmpty() ? EnhanceConfig.SUPPORT_JAVA_VERSION : version;
        return VersionExpression.of(version).match(JVM.instance().getVersion());
    }

    /**
     * Injects dependencies into instances managed by the extension manager.
     * Listens for creation events and performs injection, or logs errors if injection fails.
     */
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

    /**
     * Subscribes all available subscriptions to their respective topics on the event bus.
     * Retrieves subscriptions from the extension manager and adds handlers to the event bus.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void subscribe() {
        if (subscribers != null) {
            for (Subscriber subscriber : subscribers) {
                Subscription[] subscriptions = subscriber.subscribe();
                for (Subscription subscription : subscriptions) {
                    eventBus.subscribe(subscription);
                }
            }
        }
    }

    private List<Subscriber> createSubscribers() {
        return extensionManager.getOrLoadExtensible(Subscriber.class, classLoaderManager.getCoreImplLoader()).getExtensions();
    }

    private ExtensibleDesc<Command> createCommandManager() {
        return extensionManager.getOrLoadExtensible(Command.class, classLoaderManager.getCoreImplLoader());
    }

    /**
     * Handles various agent events and takes appropriate actions based on the event type.
     * Updates application status or logs exceptions as necessary.
     *
     * @param event the agent event to handle
     */
    private void onAgentEvent(AgentEvent event) {
        switch (event.getType()) {
            case AGENT_FAILURE:
            case AGENT_SERVICE_POLICY_FAILURE:
            case AGENT_ENHANCE_FAILURE:
                onException(event.getMessage(), event.getThrowable());
                break;
            case APPLICATION_LOADING:
                onAppLoading();
                break;
            case APPLICATION_STARTED:
                application.setStatus(AppStatus.STARTED);
                break;
            case APPLICATION_READY:
                application.setStatus(AppStatus.READY);
                break;
            case APPLICATION_CLOSE:
                application.setStatus(AppStatus.DESTROYING);
                break;
        }
    }

    /**
     * Initializes services during application loading.
     * Starts the service manager asynchronously and handles startup exceptions.
     *
     * @implNote Wraps service startup failures via {@link #onException(String, Throwable)}
     */
    private void onAppLoading() {
        logger.info("Application is loading..., classloader={}, mainClass={}", application.getClassLoader(), application.getMainClass());
        if (serviceManager != null) {
            try {
                logger.info("Start services after all agents are instrumented during application loading.");
                serviceManager.start().join();
            } catch (Throwable e) {
                onException(e.getMessage(), e);
            }
        }
    }

    /**
     * Logs an exception and optionally shuts down the application if not in dynamic mode.
     *
     * @param message   the message to log
     * @param throwable the exception to log
     */
    private void onException(String message, Throwable throwable) {
        logger.error(message, throwable);
        boolean shutdownOnError = agentConfig != null && agentConfig.getEnhanceConfig().isShutdownOnError();
        if (shutdownOnError && installed.get()) {
            logger.error("Shutdown because shutdownOnError is configured and the plugin is installed.");
            System.exit(1);
        } else if (shutdownOnError) {
            logger.error("Shutdown is not occurred because the plugin is not installed.");
            logger.error("Please fix this exception and restart the application.");
        }
    }
}
