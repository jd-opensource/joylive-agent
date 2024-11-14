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
package com.jd.live.agent.core.util;

import java.util.List;

/**
 * A utility class that provides methods to facilitate the closing of resources and threads.
 * This class is designed to be used as a singleton and supports method chaining for ease of use.
 *
 * @author Your Name
 * @since Version Number
 */
public class Close {

    /**
     * The singleton instance of the Close class.
     */
    private static final Close INSTANCE = new Close();

    /**
     * Private constructor to prevent instantiation from outside the class.
     */
    private Close() {
        // Private constructor to enforce the singleton pattern.
    }

    /**
     * Retrieves the singleton instance of the Close class.
     *
     * @return The singleton instance of the Close class.
     */
    public static Close instance() {
        return INSTANCE;
    }

    /**
     * Closes the given AutoCloseable resource and returns this Close instance for method chaining.
     * If the resource is null, this method does nothing.
     *
     * @param io The AutoCloseable resource to close.
     * @return This Close instance for method chaining.
     */
    public Close close(AutoCloseable io) {
        if (io != null) {
            try {
                io.close();
            } catch (Exception e) {
                // Optionally log the exception or handle it as needed.
            }
        }
        return this;
    }

    /**
     * Closes multiple AutoCloseable resources and returns this Close instance for method chaining.
     * If the resources array is null or contains null elements, this method does nothing for those elements.
     *
     * @param resources The AutoCloseable resources to close.
     * @return This Close instance for method chaining.
     */
    public Close close(AutoCloseable... resources) {
        if (resources != null) {
            for (AutoCloseable resource : resources) {
                if (resource != null) {
                    try {
                        resource.close();
                    } catch (Exception e) {
                        // Optionally log the exception or handle it as needed.
                    }
                }
            }
        }
        return this;
    }

    /**
     * Closes multiple AutoCloseable resources and returns this Close instance for method chaining.
     * If the resources array is null or contains null elements, this method does nothing for those elements.
     *
     * @param resources The AutoCloseable resources to close.
     * @return This Close instance for method chaining.
     */
    public Close close(List<? extends AutoCloseable> resources) {
        if (resources != null) {
            for (AutoCloseable resource : resources) {
                if (resource != null) {
                    try {
                        resource.close();
                    } catch (Exception e) {
                        // Optionally log the exception or handle it as needed.
                    }
                }
            }
        }
        return this;
    }

    /**
     * Closes the given value using the provided closer, if the value is not null.
     *
     * @param <T>    The type of the value to be closed.
     * @param value  The value to be closed.
     * @param closer The closer to use for closing the value.
     * @return This object, allowing for method chaining.
     */
    public <T> Close closeIfExists(T value, Closeable<T> closer) {
        if (value != null) {
            try {
                closer.close(value);
            } catch (Exception ignored) {
            }
        }
        return this;
    }

    /**
     * Executes the given runnable closer, if it is not null.
     *
     * @param closer The runnable closer to execute.
     * @return This object, allowing for method chaining.
     */
    public Close close(Runnable closer) {
        if (closer != null) {
            closer.run();
        }
        return this;
    }

    /**
     * Interrupts the given Thread and returns this Close instance for method chaining.
     * If the thread is null, this method does nothing.
     *
     * @param thread The Thread to interrupt.
     * @return This Close instance for method chaining.
     */
    public Close close(Thread thread) {
        if (thread != null) {
            thread.interrupt();
        }
        return this;
    }

    @FunctionalInterface
    public interface Closeable<T> {

        void close(T value) throws Exception;

    }
}

