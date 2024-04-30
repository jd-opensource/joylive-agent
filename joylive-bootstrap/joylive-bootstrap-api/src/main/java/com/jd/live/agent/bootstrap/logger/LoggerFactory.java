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
package com.jd.live.agent.bootstrap.logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A factory class for creating and managing logger instances. This class uses a bridge design pattern
 * to abstract the creation of logger instances that are backed by a specific logging framework.
 * The default implementation uses the JUL (Java Util Logging) framework.
 *
 * @since 1.0.0
 */
public abstract class LoggerFactory {

    private static volatile LoggerBridge bridge = new JULBridge();

    private static final Map<Class<?>, LoggerAdapter> loggers = new ConcurrentHashMap<>(64);

    /**
     * Retrieves a logger instance for the specified class. If a logger for the class does not already exist,
     * a new one is created and cached for future use.
     *
     * @param type The class for which the logger is to be retrieved.
     * @return A Logger instance for the specified class.
     */
    public static Logger getLogger(Class<?> type) {
        return loggers.computeIfAbsent(type == null ? LoggerFactory.class : type, t -> new LoggerAdapter(t, bridge));
    }

    /**
     * Resets the bridge to the default JULBridge, effectively resetting the logging framework to Java Util Logging.
     */
    public static void reset() {
        setBridge(new JULBridge());
    }

    /**
     * Sets a new bridge for logger creation. This allows for switching the underlying logging framework at runtime.
     * All existing loggers are updated to use the new bridge.
     *
     * @param bridge The new LoggerBridge to be used for creating loggers.
     */
    public static void setBridge(LoggerBridge bridge) {
        if (bridge != null && bridge != LoggerFactory.bridge) {
            LoggerFactory.bridge = bridge;
            for (LoggerAdapter adapter : loggers.values()) {
                adapter.setBridge(bridge);
            }
        }
    }

    /**
     * An adapter class that implements the Logger interface, delegating logging operations to a logger instance
     * obtained from the current LoggerBridge. This allows for dynamic switching of the logging framework.
     */
    protected static class LoggerAdapter implements Logger {

        private final Class<?> type;

        private LoggerBridge bridge;

        private volatile Logger delegate;

        /**
         * Constructs a new LoggerAdapter for the specified class, using the given LoggerBridge for logger creation.
         *
         * @param type   The class for which the logger is to be created.
         * @param bridge The bridge used to create the logger instance.
         */
        public LoggerAdapter(Class<?> type, LoggerBridge bridge) {
            this.type = type;
            this.bridge = bridge;
            this.delegate = bridge.getLogger(type);
        }

        /**
         * Updates the bridge used for logger creation. If the bridge is changed, a new logger instance is obtained.
         *
         * @param bridge The new LoggerBridge to use.
         */
        protected void setBridge(LoggerBridge bridge) {
            if (bridge != null && this.bridge != bridge) {
                this.bridge = bridge;
                delegate = bridge.getLogger(type);
            }
        }

        @Override
        public boolean isDebugEnabled() {
            return delegate.isDebugEnabled();
        }

        @Override
        public void debug(String message) {
            delegate.debug(message);
        }

        @Override
        public void debug(String format, Object... arguments) {
            delegate.debug(format, arguments);
        }

        @Override
        public void debug(String message, Throwable throwable) {
            delegate.debug(message, throwable);
        }

        @Override
        public boolean isInfoEnabled() {
            return delegate.isInfoEnabled();
        }

        @Override
        public void info(String message) {
            delegate.info(message);
        }

        @Override
        public void info(String format, Object... arguments) {
            delegate.info(format, arguments);
        }

        @Override
        public void info(String message, Throwable throwable) {
            delegate.info(message, throwable);
        }

        @Override
        public boolean isWarnEnabled() {
            return delegate.isWarnEnabled();
        }

        @Override
        public void warn(String message) {
            delegate.warn(message);
        }

        @Override
        public void warn(String format, Object... arguments) {
            delegate.warn(format, arguments);
        }

        @Override
        public void warn(String message, Throwable throwable) {
            delegate.warn(message, throwable);
        }

        @Override
        public boolean isErrorEnabled() {
            return delegate.isErrorEnabled();
        }

        @Override
        public void error(String message) {
            delegate.error(message);
        }

        @Override
        public void error(String format, Object... arguments) {
            delegate.error(format, arguments);
        }

        @Override
        public void error(String message, Throwable throwable) {
            delegate.error(message, throwable);
        }
    }
}
