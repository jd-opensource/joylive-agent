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
package com.jd.live.agent.bootstrap;

import com.jd.live.agent.bootstrap.classloader.CoreResourceFilter;
import com.jd.live.agent.bootstrap.classloader.LiveClassLoader;
import com.jd.live.agent.bootstrap.classloader.ResourceConfig;
import com.jd.live.agent.bootstrap.classloader.ResourcerType;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.util.Inclusion;
import com.jd.live.agent.bootstrap.util.option.ConfigResolver;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * LiveAgent
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class LiveAgent {
    private static final Logger logger = Logger.getLogger(LiveAgent.class.getName());

    private static final int STATUS_INITIAL = 0;

    private static final int STATUS_SYSTEM_LIB = 1;

    private static final int STATUS_INSTALLING = 2;

    private static final int STATUS_INSTALL_SUCCESS = 3;

    private static final int STATUS_INSTALL_FAILED = 4;

    private static final String BOOTSTRAP_CLASS = "com.jd.live.agent.core.bootstrap.Bootstrap";

    private static final String BOOTSTRAP_METHOD_INSTALL = "install";

    private static final String BOOTSTRAP_METHOD_EXECUTE = "execute";

    private static final String ARG_COMMAND = "command";

    private static final String BOOTSTRAP_PROPERTIES = "bootstrap.properties";

    private static final String SHUTDOWN_ON_ERROR = "agent.enhance.shutdownOnError";

    private static final String SUN_JAVA_COMMAND = "sun.java.command";

    private static final String EXCLUDE_APP = "agent.enhance.excludeApp";

    private static final String ARRAY_DELIMITER_PATTERN = "[;,]";

    private static final String FILE_MANIFEST_MF = "META-INF/MANIFEST.MF";

    private static final String ENV_JAVA_TOOL_OPTIONS = "JAVA_TOOL_OPTIONS";

    private static final String FILE_LIVE_AGENT = "live.jar";

    private static Object lifecycle;

    private static URLClassLoader classLoader;

    private static final AtomicInteger status = new AtomicInteger(STATUS_INITIAL);

    private static final Runnable unLoader = () -> {
        status.set(STATUS_SYSTEM_LIB);
        close(classLoader);
        classLoader = null;
        lifecycle = null;
    };

    /**
     * The `premain` method is a special method that is executed before the main application's `main` method.
     * This method is typically used for setting up instrumentation, such as class loading and transformations,
     * before the application starts.
     *
     * @param arguments       The agent startup arguments passed in, which can be passed via the
     *                        `-javaagent:youragent.jar=argument` JVM argument.
     * @param instrumentation The `Instrumentation` instance that provides class transformation and inspection tools.
     */
    public static void premain(String arguments, Instrumentation instrumentation) {
        launch(arguments, instrumentation, false);
    }

    /**
     * The `agentmain` method is called after the JVM has started and the application is already running.
     * This method allows for dynamic instrumentation of Java applications at runtime.
     *
     * @param arguments       The agent startup arguments passed in, which can be passed via
     *                        the `-agentlib` or `-agentpath` options.
     * @param instrumentation The `Instrumentation` instance that provides class transformation and inspection tools.
     */
    public static void agentmain(String arguments, Instrumentation instrumentation) {
        launch(arguments, instrumentation, true);
    }

    /**
     * Launches the agent by setting up the necessary environment, class loaders, and instrumentation.
     * This method is called by both premain and agentmain methods to initialize and start the agent.
     *
     * @param arguments       The agent startup arguments as a string.
     * @param instrumentation The {@link Instrumentation} instance provided by the JVM.
     * @param dynamic         A boolean flag indicating whether the agent was loaded dynamically
     *                        after the JVM started (true) or before the main method (false).
     */
    private static synchronized void launch(String arguments, Instrumentation instrumentation, boolean dynamic) {
        boolean shutdownOnError = true;
        try {
            InstallContext ctx = InstallContext.parse(arguments, instrumentation, dynamic);
            shutdownOnError = ctx.shutdownOnError;
            if (ctx.isInJavaToolOptions()) {
                if (ctx.isExcluded()) {
                    // Disabled logging as it breaks script-based console output parsing.
                    // logger.log(Level.WARNING, "[LiveAgent] the agent will exit when excluding main class or missing main class.");
                    return;
                } else {
                    logger.log(Level.INFO, "[LiveAgent] main class " + ctx.bootClass.name
                            + "\nif you do not want to enhance this application, "
                            + "\nyou can append this main class to \"agent.enhance.excludeApp\" in "
                            + new File(ctx.configDir, "bootstrap.properties")
                            + "\nor contact you administrator to update the default value.");
                }
            }

            // Load system libraries and set up the class loader.
            if (status.compareAndSet(STATUS_INITIAL, STATUS_SYSTEM_LIB)) {
                ctx.addSystemPath();
                ctx.configLogger();
            }
            // Install the agent by setting up the class loader and lifecycle manager.
            if (status.compareAndSet(STATUS_SYSTEM_LIB, STATUS_INSTALLING)) {
                classLoader = ctx.createClassLoader();
                lifecycle = ctx.install(classLoader);
                status.compareAndSet(STATUS_INSTALLING, lifecycle != null ? STATUS_INSTALL_SUCCESS : STATUS_INSTALL_FAILED);

                // If the installation failed and shutdownOnError is true, terminate the JVM.
                if (status.get() == STATUS_INSTALL_FAILED) {
                    onError(null, shutdownOnError);
                }
            }
            ctx.execute();
        } catch (Throwable e) {
            onError(e, shutdownOnError);
        }
    }

    private static void onError(Throwable e, boolean shutdownOnError) {
        if (e != null) {
            logger.log(Level.SEVERE, "[LiveAgent] Failed to install agent. caused by " + e.getMessage(), e);
        } else {
            logger.log(Level.SEVERE, "[LiveAgent] Failed to install agent.");
        }
        if (shutdownOnError) {
            System.exit(1);
        }
    }

    /**
     * Closes the specified URLClassLoader.
     *
     * @param classLoader The URLClassLoader to be closed.
     */
    private static void close(URLClassLoader classLoader) {
        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (IOException ignore) {
                // Ignoring IOException during class loader close operation.
            }
        }
    }

    /**
     * A class representing the arguments for installing and configuring the agent.
     */
    private static class InstallContext {
        private Instrumentation instrumentation;
        private boolean dynamic;
        private File root;
        private Map<String, Object> args;
        private File configDir;
        private File libDir;
        private File[] systemLibs;
        private File[] coreLibs;
        private URL[] coreLibUrls;
        private Map<String, Object> env;
        private Map<String, Object> bootstrapConfig;
        private ConfigResolver configuration;
        private String command;
        private boolean shutdownOnError;
        private BootClass bootClass;
        private Inclusion exclusion;

        /**
         * Parses the installation arguments and returns an InstallArg object.
         *
         * @param arguments       The installation arguments as a string.
         * @param instrumentation The instrumentation object used for agent installation.
         * @param dynamic         Whether to perform dynamic instrumentation or not.
         * @return An InstallArg object containing the parsed arguments.
         * @throws IOException If there is an error parsing the arguments.
         */
        public static InstallContext parse(String arguments, Instrumentation instrumentation, boolean dynamic) throws IOException {
            InstallContext arg = new InstallContext();
            arg.instrumentation = instrumentation;
            arg.dynamic = dynamic;
            arg.args = createArgs(arguments);
            arg.env = createEnv();
            arg.command = (String) arg.args.get(ARG_COMMAND);
            arg.bootClass = getBootClass();

            arg.root = LivePath.getRootPath(arg.env, arg.args);
            if (arg.root != null) {
                // Update the environment with the determined agent path.
                arg.env.put(LivePath.KEY_AGENT_PATH, arg.root.getPath());
            }
            arg.configDir = new File(arg.root, LivePath.DIR_CONFIG);
            arg.libDir = new File(arg.root, LivePath.DIR_LIB);
            arg.systemLibs = getLibs(new File(arg.libDir, LivePath.DIR_LIB_SYSTEM), true);
            arg.coreLibs = getLibs(new File(arg.libDir, LivePath.DIR_LIB_CORE), false);
            arg.coreLibUrls = getUrls(arg.coreLibs);

            arg.bootstrapConfig = createBootstrapConfig(arg.configDir);
            // Configure the agent based on the bootstrap configuration and environment.
            arg.configuration = new ConfigResolver(arg.bootstrapConfig, arg.env);
            arg.shutdownOnError = isShutdownOnError(arg.configuration);

            // Exclude apps
            arg.exclusion = getExcludeApps(arg.configuration);
            return arg;
        }

        /**
         * Gets the main class of the current application.
         *
         * @return The main class as a string.
         */
        private static BootClass getBootClass() {
            String command = System.getProperty(SUN_JAVA_COMMAND);
            if (command != null) {
                int index = command.indexOf(" ");
                if (index != -1) {
                    command = command.substring(0, index);
                }
                if (command.endsWith(".jar")) {
                    command = getMainClassByJar(command);
                }
            }
            return command == null ? null : new BootClass(command);
        }

        /**
         * Gets the main class from a JAR file.
         *
         * @param file The path to the JAR file.
         * @return The main class as a string.
         */
        private static String getMainClassByJar(String file) {
            File jarfile = new File(file);
            if (jarfile.exists()) {
                try (JarFile jar = new JarFile(jarfile)) {
                    Manifest manifest = jar.getManifest();
                    if (manifest != null) {
                        String mainClass = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
                        if (mainClass != null) {
                            return mainClass;
                        }
                    }
                } catch (IOException e) {
                    // Disabled logging as it breaks script-based console output parsing.
                    // logger.log(Level.SEVERE, "[LiveAgent] Failed to read main class from " + file, e);
                    return null;
                }
            }
            // Disabled logging as it breaks script-based console output parsing.
            // logger.log(Level.SEVERE, "[LiveAgent] Failed to find jar:" + file);
            return null;
        }

        /**
         * Checks if the main class of the current application is excluded from monitoring.
         *
         * @return true if the main class is excluded, false otherwise.
         */
        public boolean isExcluded() {
            return bootClass == null || bootClass.isExclude(exclusion);
        }

        /**
         * Checks if the Java tool options environment variable contains the Live Agent file.
         *
         * @return true if the Java tool options contain the Live Agent file, false otherwise.
         */
        public boolean isInJavaToolOptions() {
            String value = (String) env.get(ENV_JAVA_TOOL_OPTIONS);
            return value != null && value.contains(FILE_LIVE_AGENT);
        }

        /**
         * Adds the system libraries to the bootstrap class loader search.
         *
         * @throws IOException If there is an error reading the system libraries.
         */
        public void addSystemPath() throws IOException {
            if (systemLibs != null) {
                for (File file : systemLibs) {
                    try (JarFile jarFile = new JarFile(file)) {
                        instrumentation.appendToBootstrapClassLoaderSearch(jarFile);
                    }
                }
            }
        }

        /**
         * Configures the logger to use a custom log handler and disables the use of parent handlers.
         */
        public void configLogger() {
            logger.setUseParentHandlers(false);
            logger.addHandler(new LogHandler());
        }

        /**
         * Executes a command if the agent installation status allows it.
         */
        public void execute() {
            // Execute the command if provided and the agent installation status allows it.
            if (command != null && !command.isEmpty()) {
                switch (status.get()) {
                    case STATUS_INSTALL_SUCCESS:
                    case STATUS_INSTALL_FAILED:
                        execute(lifecycle);
                        break;
                    default:
                        execute(null);
                }
            }
        }

        /**
         * Creates a class loader with specified URLs, configuration path, and a configuration function.
         *
         * @return A URLClassLoader instance that can load classes and resources from the specified URLs.
         */
        public URLClassLoader createClassLoader() {
            // Create a new ResourceConfig using the configuration function and a prefix.
            ResourceConfig config = new ResourceConfig(configuration, ResourceConfig.CORE_PREFIX);
            // Instantiate a new CoreResourceFilter with the created configuration and the config path.
            CoreResourceFilter filter = new CoreResourceFilter(config, configDir);
            // Return a new instance of LiveClassLoader with the provided URLs and filter.
            return new LiveClassLoader(coreLibUrls, Thread.currentThread().getContextClassLoader(), ResourcerType.CORE, filter);
        }

        /**
         * Installs the agent by loading the bootstrap class and invoking its install method.
         *
         * @param classLoader The class loader to use for loading the agent's classes.
         * @return An object representing the agent's lifecycle, or null if the installation fails.
         */
        public Object install(ClassLoader classLoader) {
            try {
                logger.info("[LiveAgent] Installing agent.");
                // Load the bootstrap class using the provided class loader.
                Class<?> type = classLoader.loadClass(BOOTSTRAP_CLASS);
                // Get the constructor of the bootstrap class.
                Constructor<?> constructor = type.getConstructor(Instrumentation.class, boolean.class, Map.class, Map.class, Runnable.class);
                // Instantiate the bootstrap class with the given parameters.
                Object lifecycle = constructor.newInstance(instrumentation, dynamic, env, bootstrapConfig, unLoader);
                // Get the installation method from the bootstrap class.
                Method install = type.getDeclaredMethod(BOOTSTRAP_METHOD_INSTALL);
                // Invoke the installation method to complete the agent installation.
                install.invoke(lifecycle);
                // Return the lifecycle object for further operations.
                return lifecycle;
            } catch (InvocationTargetException e) {
                // Log the exception thrown by the installation method and exit.
                String message = e.getMessage();
                message = message == null ? e.getTargetException().getMessage() : message;
                logger.log(Level.SEVERE, "Failed to install agent. caused by " + message);
            } catch (Throwable e) {
                // Log any other exceptions that occurred during the installation process and exit.
                logger.log(Level.SEVERE, "Failed to install agent. caused by " + e.getMessage(), e);
            }
            // If the installation fails, return null.
            return null;
        }

        /**
         * Executes a command on the target object using reflection.
         *
         * @param target The object on which the command is to be executed, or null if the agent is not installed.
         */
        private void execute(Object target) {
            logger.info("executing command " + command);
            if (target != null) {
                try {
                    // Reflectively obtain the method to execute the command.
                    Method execute = target.getClass().getDeclaredMethod(BOOTSTRAP_METHOD_EXECUTE, String.class, Map.class);
                    // Invoke the command with the provided arguments.
                    execute.invoke(target, command, args);
                    logger.info("success executing command " + command);
                } catch (InvocationTargetException e) {
                    // Log the exception thrown by the method being invoked.
                    logger.log(Level.SEVERE, "failed to execute command " + command + ", caused by " + e.getTargetException().getMessage(), e.getTargetException());
                } catch (Throwable e) {
                    // Log other exceptions that occurred during reflection or invocation.
                    logger.log(Level.SEVERE, "failed to execute command " + command + ", caused by " + e.getMessage(), e);
                }
            } else {
                // If the agent is not installed, log a message indicating that execution cannot proceed.
                logger.info("agent is not successfully installed, please retry later.");
            }
        }

        /**
         * Parses a string of arguments into a map where each key-value pair is separated by an equal sign,
         * and different pairs are separated by a semicolon.
         *
         * @param args The string containing the arguments to parse.
         * @return A map with the parsed arguments.
         */
        private static Map<String, Object> createArgs(String args) {
            // Create a new HashMap to store the arguments.
            Map<String, Object> result = new HashMap<>();
            if (args != null) {
                // Split the input string into parts using the semicolon as a delimiter.
                String[] parts = args.trim().split(ARRAY_DELIMITER_PATTERN);
                for (String arg : parts) {
                    // Find the index of the equal sign to separate key and value.
                    int index = arg.indexOf('=');
                    if (index > 0) { // Ensure that there is a key before the equal sign.
                        // Extract the key and value, trimming any whitespace.
                        String key = arg.substring(0, index).trim();
                        String value = arg.substring(index + 1).trim();
                        // If the value is not empty, put the key-value pair into the map.
                        if (!value.isEmpty()) {
                            result.put(key, value);
                        }
                    }
                }
            }
            // Return the map with the parsed arguments.
            return result;
        }

        /**
         * Creates an environment map that contains all system properties and environment variables.
         *
         * @return A map with system properties and environment variables.
         */
        private static Map<String, Object> createEnv() {
            // in line with Spring’s ordering
            // Create a new HashMap to store the environment variables and system properties.
            // Add all environment variables to the map.
            Map<String, Object> result = new HashMap<>(System.getenv());
            // Add all system properties to the map.
            for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
                result.put(entry.getKey().toString(), entry.getValue());
            }
            // Add argument properties to the map.
            String command = System.getProperty("sun.java.command");
            if (command != null && !command.isEmpty()) {
                String[] args = command.split(" ");
                for (String arg : args) {
                    if (arg.startsWith("--")) {
                        int index = arg.indexOf('=');
                        if (index > 2) {
                            String key = arg.substring(2, index);
                            String value = arg.substring(index + 1);
                            if (!value.isEmpty()) {
                                result.put(key, value);
                            }
                        }
                    }
                }
            }
            // Return the map containing both system properties and environment variables.
            return result;
        }

        /**
         * Creates a configuration map from a properties file located in the specified configuration directory.
         *
         * @param configDir The directory where the configuration properties file is located.
         * @return A map with the configuration loaded from the properties file.
         * @throws IOException If an I/O error occurs while reading the properties file.
         */
        private static Map<String, Object> createBootstrapConfig(File configDir) throws IOException {
            // Create a new HashMap to store the bootstrap configuration.
            Map<String, Object> result = new HashMap<>();
            // Construct a file object for the bootstrap properties file.
            File file = new File(configDir, BOOTSTRAP_PROPERTIES);
            if (file.exists()) {
                // If the file exists, read it using a FileReader wrapped in a BufferedReader.
                try (Reader reader = new BufferedReader(new FileReader(file))) {
                    Properties properties = new Properties();
                    // Load the properties from the reader.
                    properties.load(reader);
                    // Put all properties into the result map.
                    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                        result.put(entry.getKey().toString(), entry.getValue());
                    }
                }
            }
            // Return the map containing the bootstrap configuration.
            return result;
        }

        /**
         * Determines whether the agent should shut down the JVM on error based on the provided environment settings.
         *
         * @param env A function that retrieves environment settings based on a given key.
         * @return A boolean indicating whether the JVM should be shut down on error.
         */
        private static boolean isShutdownOnError(Function<String, Object> env) {
            String value = (String) env.apply(SHUTDOWN_ON_ERROR);
            if (value != null) {
                try {
                    return Boolean.parseBoolean(value);
                } catch (Exception ignore) {
                    // If there's an exception parsing the boolean value, ignore it and return the default value.
                }
            }
            // Default to true if the value is not set or cannot be parsed.
            return true;
        }

        /**
         * Retrieves an array of JAR files located in a specified directory.
         *
         * @param dir   The directory to search for JAR files.
         * @param empty Whether the directory is allowed to be empty or not.
         * @return An array of File objects representing the JAR files found in the directory.
         * @throws IOException If the directory does not exist, is not a directory, or is empty (and empty is false).
         */
        private static File[] getLibs(File dir, boolean empty) throws IOException {
            if (!empty && !dir.exists() && !dir.isDirectory()) {
                throw new IOException("Directory is not exists." + dir.getPath());
            }
            File[] files = dir.listFiles(pathname -> pathname.isFile() && pathname.getName().endsWith(".jar"));
            if (!empty && (files == null || files.length == 0)) {
                throw new IOException("Directory is empty. " + dir);
            }
            return files;
        }

        /**
         * Converts an array of File objects into an array of URL objects.
         *
         * @param libs The array of File objects to be converted.
         * @return An array of URL objects corresponding to the input File objects.
         * @throws IOException If an error occurs while converting File to URL.
         */
        private static URL[] getUrls(File[] libs) throws IOException {
            URL[] urls = new URL[libs.length];
            for (int i = 0; i < libs.length; i++) {
                urls[i] = libs[i].toURI().toURL();
            }
            return urls;
        }

        /**
         * Retrieves the set of excluded applications from the configuration.
         *
         * @param env The environment function used to retrieve configuration values.
         * @return A set of strings representing the excluded applications.
         */
        private static Inclusion getExcludeApps(Function<String, Object> env) {
            Inclusion result = new Inclusion();
            String config = (String) env.apply(EXCLUDE_APP);
            if (config != null) {
                String[] values = config.split(ARRAY_DELIMITER_PATTERN);
                for (String value : values) {
                    result.addClassName(value);
                }
            }
            return result;
        }

    }

    /**
     * Represents a boot class in the Java Virtual Machine.
     */
    private static class BootClass {
        private final String name;
        private final String module;
        private final String className;

        BootClass(String name) {
            this.name = name;
            // jdk.jcmd/sun.tools.jstack.JStack
            // jdk.jcmd/sun.tools.jmap.JMap
            // jdk.compiler/com.sun.tools.javac.Main
            int pos = name.lastIndexOf('/');
            this.module = pos > 0 ? name.substring(0, pos) : null;
            this.className = pos > 0 ? name.substring(pos + 1) : name;
        }

        /**
         * Determines if this class should be excluded based on the provided predicate.
         *
         * @param predicate the condition to test against class names (must not be null)
         * @return true if the class should be excluded according to the above rules
         */
        public boolean isExclude(Predicate<String> predicate) {
            if (isJdkTool() || predicate.test(name)) {
                return true;
            }
            return module != null && predicate.test(className);
        }

        /**
         * Checks if the boot class belongs to a JDK tool.
         *
         * @return true if the boot class belongs to a JDK tool, false otherwise.
         */
        private boolean isJdkTool() {
            return module != null && module.startsWith("jdk.")
                    || className != null && (className.startsWith("sun.")
                    || className.startsWith("com.sun.")
                    || className.startsWith("jdk.")
            );
        }

    }

    private static class LogHandler extends Handler {
        final com.jd.live.agent.bootstrap.logger.Logger delegate = LoggerFactory.getLogger(LogHandler.class);

        @Override
        public void publish(LogRecord record) {
            Level level = record.getLevel();
            if (level == Level.SEVERE) {
                delegate.error(record.getMessage());
            } else if (level == Level.INFO) {
                delegate.info(record.getMessage());
            } else if (level == Level.WARNING) {
                delegate.warn(record.getMessage());
            } else if (level == Level.FINE) {
                delegate.debug(record.getMessage());
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }

}
