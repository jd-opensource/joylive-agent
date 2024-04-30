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
package com.jd.live.agent.core.service;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.exception.InitialTimeoutException;
import com.jd.live.agent.core.util.Daemon;
import com.jd.live.agent.core.util.Waiter;

import java.util.concurrent.CompletableFuture;

/**
 * AbstractSyncService provides a base implementation for synchronous services. It extends the
 * {@link AbstractService} and is designed to handle synchronization tasks with configurable
 * intervals and fault tolerance. It uses a daemon thread to perform synchronization and offers
 * methods to tailor the synchronization logic to specific needs.
 *
 * @param <T> The type of data to be synchronized.
 * @param <M> The type of metadata associated with the synchronization.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public abstract class AbstractSyncService<T, M> extends AbstractService {

    /**
     * Logger instance for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(AbstractSyncService.class);

    /**
     * The daemon thread responsible for executing synchronization tasks.
     */
    protected Daemon daemon;

    /**
     * The synchronization configuration.
     */
    protected SyncConfig config;

    /**
     * The last metadata object obtained from the synchronization process.
     */
    protected M last;

    /**
     * The waiter used to block and unblock the daemon thread.
     */
    protected Waiter.MutexWaiter waiter;

    /**
     * Starts the synchronization service by initializing the daemon thread and initiating its execution.
     *
     * @return A CompletableFuture that is completed when the service has started.
     */
    @Override
    protected CompletableFuture<Void> doStart() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        waiter = new Waiter.MutexWaiter();
        config = getSyncConfig();
        daemon = createDaemon(future);
        daemon.start();
        return future;
    }

    /**
     * Stops the synchronization service by waking up the waiter and stopping the daemon thread.
     *
     * @return A CompletableFuture that is completed when the service has stopped.
     */
    @Override
    protected CompletableFuture<Void> doStop() {
        if (waiter != null) {
            waiter.wakeup();
        }
        if (daemon != null) {
            daemon.stop();
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Generates a random long value based on the given time and span.
     *
     * @param time The base time value.
     * @param span The range within which the random value is generated.
     * @return A random long value within the specified range.
     */
    protected long random(long time, int span) {
        return time + (int) (Math.random() * span);
    }

    /**
     * Creates a daemon thread with the specified configuration.
     *
     * @param future The CompletableFuture to complete when the initial synchronization is done.
     * @return A new Daemon instance configured according to the provided parameters.
     */
    protected Daemon createDaemon(CompletableFuture<Void> future) {
        // Reduce pressure by adding a random time component.
        return Daemon.builder()
                .name(getName())
                .delay(getDelay())
                .fault(getFault())
                .interval(getInterval())
                .waiter(waiter)
                .condition(this::isStarted)
                .runnable(() -> run(System.currentTimeMillis(), future))
                .build();
    }

    /**
     * Retrieves the fault tolerance value from the configuration.
     *
     * @return The fault tolerance value.
     */
    protected long getFault() {
        return config.getFault();
    }

    /**
     * Retrieves the interval value from the configuration, with a random adjustment.
     *
     * @return The randomized interval value.
     */
    protected long getInterval() {
        return random(config.getInterval(), 2000);
    }

    /**
     * Retrieves the delay value from the configuration, with a random adjustment.
     *
     * @return The randomized delay value.
     */
    protected long getDelay() {
        return random(config.getDelay(), 1000);
    }

    /**
     * The main run method executed by the daemon thread. It performs synchronization and updates the
     * state based on the result.
     *
     * @param startTime      The start time of the run method.
     * @param waitForInitial The CompletableFuture to complete when the initial synchronization is done.
     */
    protected void run(long startTime, CompletableFuture<Void> waitForInitial) {
        Throwable throwable = null;
        boolean synced = false;
        try {
            synced = syncAndUpdate();
        } catch (Throwable e) {
            logger.error("failed to sync and update " + getName(), e);
            throwable = e;
        }
        if (!waitForInitial.isDone()) {
            long timeout = config.getInitialTimeout();
            if (timeout > 0 && (System.currentTimeMillis() - startTime > timeout)) {
                waitForInitial.completeExceptionally(new InitialTimeoutException("it's timeout to initialize " + getName() +
                        (throwable == null ? "" : ", caused by " + throwable.getMessage())));
            } else if (synced) {
                waitForInitial.complete(null);
            }
        }
    }

    /**
     * Performs synchronization and updates the state once.
     *
     * @return True if the synchronization and update were successful, false otherwise.
     * @throws Exception If an error occurs during synchronization.
     */
    protected boolean syncAndUpdate() throws Exception {
        SyncResult<T, M> result = sync(config, last);
        if (result != null) {
            last = result.getMeta();
            for (int i = 0; i < UPDATE_MAX_RETRY; i++) {
                if (updateOnce(result.getData(), result.getMeta())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Retrieves the synchronization configuration.
     *
     * @return The SyncConfig instance.
     */
    protected abstract SyncConfig getSyncConfig();

    /**
     * Updates the state with the new data and metadata.
     *
     * @param value The new data to update.
     * @param meta The new metadata to update.
     * @return True if the update was successful, false otherwise.
     */
    protected abstract boolean updateOnce(T value, M meta);

    /**
     * Performs synchronization based on the provided configuration and the last metadata.
     *
     * @param config The SyncConfig instance.
     * @param last The last metadata object.
     * @return A SyncResult instance containing the new data and metadata.
     * @throws Exception If an error occurs during synchronization.
     */
    protected abstract SyncResult<T, M> sync(SyncConfig config, M last) throws Exception;

    /**
     * Concatenates a URL with a single path, handling edge cases for slashes.
     *
     * @param url The base URL.
     * @param path The path to append.
     * @return The concatenated URL.
     */
    protected String concat(String url, String path) {
        if (path == null) {
            return null;
        } else if (url.endsWith("/")) {
            return url + (path.startsWith("/") ? path.substring(1) : path);
        } else {
            return url + (path.startsWith("/") ? path : "/" + path);
        }
    }

    /**
     * Concatenates a URL with multiple paths, handling edge cases for slashes.
     *
     * @param url The base URL.
     * @param paths The paths to append.
     * @return The concatenated URL.
     */
    protected String concat(String url, String... paths) {
        if (paths == null) {
            return url;
        }
        String result = url;
        for (String path : paths) {
            result = concat(result, path);
        }
        return result;
    }
}

