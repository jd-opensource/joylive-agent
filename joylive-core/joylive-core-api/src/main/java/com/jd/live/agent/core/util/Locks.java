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

import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Utility class for simplified read/write lock operations.
 * <p>
 * Provides convenience methods to execute code blocks within read/write locks,
 * automatically handling lock acquisition and release.
 */
public class Locks {

    /**
     * Executes a Runnable within a read lock.
     *
     * @param lock     the ReadWriteLock to use
     * @param runnable the action to execute under read lock
     */
    public static void read(ReadWriteLock lock, Runnable runnable) {
        lock.readLock().lock();
        try {
            runnable.run();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Executes a Callable within a read lock.
     *
     * @param <T>      the result type
     * @param lock     the ReadWriteLock to use
     * @param callable the computation to execute under read lock
     * @return the computed result
     * @throws Exception if the computation throws an exception
     */
    public static <T> T read(ReadWriteLock lock, Callable<T> callable) throws Exception {
        lock.readLock().lock();
        try {
            return callable.call();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Executes a Runnable within a write lock.
     *
     * @param lock     the ReadWriteLock to use
     * @param runnable the action to execute under write lock
     */
    public static void write(ReadWriteLock lock, Runnable runnable) {
        lock.writeLock().lock();
        try {
            runnable.run();
        } finally {
            lock.writeLock().unlock();  // Fixed typo: was readLock().unlock()
        }
    }

    /**
     * Executes a Callable within a write lock.
     *
     * @param <T>      the result type
     * @param lock     the ReadWriteLock to use
     * @param callable the computation to execute under write lock
     * @return the computed result
     * @throws Exception if the computation throws an exception
     */
    public static <T> T write(ReadWriteLock lock, Callable<T> callable) throws Exception {
        lock.writeLock().lock();
        try {
            return callable.call();
        } finally {
            lock.writeLock().unlock();  // Fixed typo: was readLock().unlock()
        }
    }
}
