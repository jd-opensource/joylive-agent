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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.Level;

/**
 * An adapter class that implements the custom Logger interface, delegating logging operations to the Java Util Logging (JUL) framework.
 * This allows for integrating JUL with systems that use the generic Logger interface for logging.
 * The class translates logging levels and methods from the Logger interface to their equivalents in JUL.
 *
 * @since 1.0.0
 */
public class JULLogger implements Logger {

    private final java.util.logging.Logger delegate;

    /**
     * Constructs a JULLogger instance that delegates logging operations to the specified JUL Logger.
     *
     * @param delegate A java.util.logging.Logger instance to which logging operations are delegated.
     */
    public JULLogger(java.util.logging.Logger delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isDebugEnabled() {
        return delegate.isLoggable(Level.FINE);
    }

    @Override
    public void debug(String message) {
        delegate.log(Level.FINE, message);
    }

    @Override
    public void debug(String format, Object... arguments) {
        delegate.log(Level.FINE, format, arguments);
    }

    @Override
    public void debug(String message, Throwable throwable) {
        delegate.log(Level.FINE, printStackTrace(message, throwable));
    }

    @Override
    public boolean isInfoEnabled() {
        return delegate.isLoggable(Level.INFO);
    }

    @Override
    public void info(String message) {
        delegate.log(Level.INFO, message);
    }

    @Override
    public void info(String format, Object... arguments) {
        delegate.log(Level.INFO, format, arguments);
    }

    @Override
    public void info(String message, Throwable throwable) {
        delegate.log(Level.INFO, printStackTrace(message, throwable));
    }

    @Override
    public boolean isWarnEnabled() {
        return delegate.isLoggable(Level.WARNING);
    }

    @Override
    public void warn(String message) {
        delegate.log(Level.WARNING, message);
    }

    @Override
    public void warn(String format, Object... arguments) {
        delegate.log(Level.WARNING, format, arguments);
    }

    @Override
    public void warn(String message, Throwable throwable) {
        delegate.log(Level.WARNING, printStackTrace(message, throwable));
    }

    @Override
    public boolean isErrorEnabled() {
        return delegate.isLoggable(Level.SEVERE);
    }

    @Override
    public void error(String message) {
        delegate.log(Level.SEVERE, message);
    }

    @Override
    public void error(String format, Object... arguments) {
        delegate.log(Level.SEVERE, format, arguments);
    }

    @Override
    public void error(String message, Throwable throwable) {
        delegate.log(Level.SEVERE, printStackTrace(message, throwable));
    }

    /**
     * Generates a string representation of a message followed by the stack trace of a Throwable.
     *
     * @param message   The message to precede the stack trace.
     * @param throwable The throwable whose stack trace is to be obtained. If null, only the message is returned.
     * @return A string containing the message and the stack trace of the provided Throwable.
     */
    protected String printStackTrace(String message, Throwable throwable) {
        if (throwable == null) {
            return message;
        }
        OutputStream bos = new ByteArrayOutputStream(1024);
        PrintStream printStream = new PrintStream(bos);
        printStream.println(message);
        throwable.printStackTrace(printStream);
        return bos.toString();
    }
}
