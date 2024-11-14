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
package com.jd.live.agent.core.service.sync;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.exception.InitialTimeoutException;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.Daemon;
import com.jd.live.agent.core.util.Waiter.MutexWaiter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AbstractSyncer provides a base implementation for synchronous services. It extends the
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
public abstract class AbstractSyncer<T, M> extends AbstractService {

    private static final int INTERVALS = 10;

    /**
     * Logger instance for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(AbstractSyncer.class);

    /**
     * The daemon thread responsible for executing synchronization tasks.
     */
    protected Daemon daemon;

    /**
     * The last metadata object obtained from the synchronization process.
     */
    protected M last;

    /**
     * The waiter used to block and unblock the daemon thread.
     */
    protected MutexWaiter waiter;

    protected final AtomicLong counter = new AtomicLong(0);

    /**
     * Starts the synchronization service by initializing the daemon thread and initiating its execution.
     *
     * @return A CompletableFuture that is completed when the service has started.
     */
    @Override
    protected CompletableFuture<Void> doStart() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        waiter = new MutexWaiter();
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
        Close.instance().closeIfExists(waiter, MutexWaiter::wakeup).closeIfExists(daemon, Daemon::stop);
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
        return getSyncConfig().getFault();
    }

    /**
     * Retrieves the interval value from the configuration, with a random adjustment.
     *
     * @return The randomized interval value.
     */
    protected long getInterval() {
        return random(getSyncConfig().getInterval(), 2000);
    }

    /**
     * Retrieves the delay value from the configuration, with a random adjustment.
     *
     * @return The randomized delay value.
     */
    protected long getDelay() {
        return random(getSyncConfig().getDelay(), 1000);
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
        counter.incrementAndGet();
        try {
            synced = syncAndUpdate();
        } catch (Throwable e) {
            onFailed(e);
            throwable = e;
        }
        if (!waitForInitial.isDone()) {
            long timeout = getSyncConfig().getInitialTimeout();
            if (timeout > 0 && (System.currentTimeMillis() - startTime > timeout)) {
                waitForInitial.completeExceptionally(new InitialTimeoutException("It's timeout to initialize " + getName() +
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
        SyncResult<T, M> result = doSynchronize(last);
        if (result != null) {
            last = result.getMeta();
            if (update(result.getData(), result.getMeta())) {
                onUpdated();
                return true;
            }
        } else {
            onNotModified();
        }
        return false;
    }

    /**
     * Determines whether the current state should trigger an output.
     *
     * @return {@code true} if the current count matches the output interval,
     * indicating that an output action should be performed; {@code false} otherwise.
     */
    protected boolean shouldPrint() {
        return counter.get() % INTERVALS == 1;
    }

    /**
     * Handles the updated state.
     */
    protected void onUpdated() {

    }

    /**
     * Handles the scenario where no modifications are detected.
     */
    protected void onNotModified() {

    }

    /**
     * Handles failure scenarios during synchronization and update processes.
     *
     * @param throwable The exception or error that caused the failure.
     */
    protected void onFailed(Throwable throwable) {
        logger.error("Failed to synchronize and update " + getName() + ". caused by " + throwable.getMessage(), throwable);
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
    protected abstract boolean update(T value, M meta);

    /**
     * Performs synchronization based on the provided the last metadata.
     *
     * @param last The last metadata object.
     * @return A SyncResult instance containing the new data and metadata.
     * @throws Exception If an error occurs during synchronization.
     */
    protected abstract SyncResult<T, M> doSynchronize(M last) throws Exception;
}

