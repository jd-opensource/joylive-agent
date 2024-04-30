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

/**
 * A Logger interface designed for handling logging across different levels such as debug, info, warn, and error.
 * It provides methods to log messages with or without formatting arguments and throwable details.
 *
 * @since 1.0.0
 */
public interface Logger {

    /**
     * Checks if debug level logging is enabled.
     *
     * @return true if debug level logging is enabled, false otherwise.
     */
    boolean isDebugEnabled();

    /**
     * Logs a debug level message.
     *
     * @param message the message to log.
     */
    void debug(String message);

    /**
     * Logs a formatted debug level message.
     *
     * @param format    the format string.
     * @param arguments the arguments for the format string.
     */
    void debug(String format, Object... arguments);

    /**
     * Logs a debug level message along with a throwable.
     *
     * @param message   the message to log.
     * @param throwable the throwable to log.
     */
    void debug(String message, Throwable throwable);

    /**
     * Checks if info level logging is enabled.
     *
     * @return true if info level logging is enabled, false otherwise.
     */
    boolean isInfoEnabled();

    /**
     * Logs an info level message.
     *
     * @param message the message to log.
     */
    void info(String message);

    /**
     * Logs a formatted info level message.
     *
     * @param format    the format string.
     * @param arguments the arguments for the format string.
     */
    void info(String format, Object... arguments);

    /**
     * Logs an info level message along with a throwable.
     *
     * @param message   the message to log.
     * @param throwable the throwable to log.
     */
    void info(String message, Throwable throwable);

    /**
     * Checks if warn level logging is enabled.
     *
     * @return true if warn level logging is enabled, false otherwise.
     */
    boolean isWarnEnabled();

    /**
     * Logs a warn level message.
     *
     * @param message the message to log.
     */
    void warn(String message);

    /**
     * Logs a formatted warn level message.
     *
     * @param format    the format string.
     * @param arguments the arguments for the format string.
     */
    void warn(String format, Object... arguments);

    /**
     * Logs a warn level message along with a throwable.
     *
     * @param message   the message to log.
     * @param throwable the throwable to log.
     */
    void warn(String message, Throwable throwable);

    /**
     * Checks if error level logging is enabled.
     *
     * @return true if error level logging is enabled, false otherwise.
     */
    boolean isErrorEnabled();

    /**
     * Logs an error level message.
     *
     * @param message the message to log.
     */
    void error(String message);

    /**
     * Logs a formatted error level message.
     *
     * @param format    the format string.
     * @param arguments the arguments for the format string.
     */
    void error(String format, Object... arguments);

    /**
     * Logs an error level message along with a throwable.
     *
     * @param message   the message to log.
     * @param throwable the throwable to log.
     */
    void error(String message, Throwable throwable);

}

