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
package com.jd.live.agent.governance.policy;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.policy.service.ServiceName;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Represents a subscriber to a policy, encapsulating the subscription details and providing mechanisms to track
 * the completion status of the subscription. It allows for asynchronous notification upon completion or failure
 * of the policy subscription process.
 */
@Getter
public class PolicySubscription implements ServiceName {

    private static final Logger logger = LoggerFactory.getLogger(PolicySubscription.class);

    private final String name;

    private final String namespace;

    private final String fullName;

    private final String type;

    private final Map<String, AtomicBoolean> syncers;

    private final AtomicInteger counter;

    private final AtomicReference<SyncState> state = new AtomicReference<>(null);

    private final List<CompletableFuture<Void>> futures = new CopyOnWriteArrayList<>();

    private final Object mutex = new Object();

    /**
     * Constructs a new instance of a policy subscriber with the specified name, namespace, and policy type.
     *
     * @param name      The name of the subscriber.
     * @param namespace The namespace of the subscriber.
     * @param type      The type of the subscriber.
     * @param syncers   The owner of the subscriber.
     */
    public PolicySubscription(String name, String namespace, String type, List<String> syncers) {
        this.name = name;
        this.namespace = namespace;
        this.fullName = ServiceName.getUniqueName(namespace, name);
        this.type = type;
        this.syncers = syncers == null || syncers.isEmpty() ? null
                : syncers.stream().collect(Collectors.toMap(o -> o, o -> new AtomicBoolean(false)));
        this.counter = new AtomicInteger(this.syncers == null ? 0 : this.syncers.size());
    }

    /**
     * Completes the synchronization process for the specified syncer.
     *
     * @param syncer The name of the syncer to complete.
     * @return true if the syncer was successfully completed, false otherwise.
     */
    public boolean complete(String syncer) {
        if (syncer == null || syncers == null) {
            return complete();
        }
        AtomicBoolean done = syncers.get(syncer);
        if (done != null && done.compareAndSet(false, true)) {
            logger.info("Success fetching {} {} governance policy by {}.", fullName, type, syncer);
            if (counter.decrementAndGet() == 0) {
                completeAndRun(() -> onComplete(future -> future.complete(null)));
            }
            return true;
        }
        return false;
    }

    /**
     * Completes the synchronization process.
     *
     * @return true if the synchronization process was successfully completed, false otherwise.
     */
    public boolean complete() {
        return completeAndRun(() -> {
            if (syncers != null) {
                syncers.forEach((k, v) -> v.compareAndSet(false, true));
            }
            counter.set(0);
            onComplete(future -> future.complete(null));
        });
    }

    /**
     * Completes the synchronization process exceptionally with the given throwable.
     *
     * @param ex The throwable that caused the exceptional completion.
     * @return true if the synchronization process was successfully completed exceptionally, false otherwise.
     */
    public boolean completeExceptionally(Throwable ex) {
        if (state.compareAndSet(null, new SyncState(ex))) {
            onComplete(future -> future.completeExceptionally(ex));
            return true;
        }
        return false;
    }

    /**
     * Watches the result of an asynchronous operation and returns a future that will be completed when the operation succeeds.
     *
     * @return a future that will be completed with null when the operation succeeds
     */
    public CompletableFuture<Void> watch() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        synchronized (mutex) {
            SyncState sr = state.get();
            if (sr != null && sr.isSuccess()) {
                future.complete(null);
            } else {
                futures.add(future);
            }
        }
        return future;
    }

    /**
     * Checks if the current state indicates readiness for operation.
     *
     * @return {@code true} if the current state exists and represents a successful state,
     * {@code false} otherwise
     */
    public boolean isReady() {
        SyncState syncState = state.get();
        return syncState != null && syncState.isSuccess();
    }

    public List<String> getUnCompletedSyncers() {
        List<String> unComplete = new ArrayList<>();
        if (syncers != null) {
            syncers.forEach((k, v) -> {
                if (!v.get()) {
                    unComplete.add(k);
                }
            });
        }
        return unComplete;
    }

    /**
     * Atomically completes the current operation and executes the given action if successful.
     *
     * @param runnable the action to execute upon successful completion (may be null)
     * @return true if state transition was successful, false if already completed
     */
    private boolean completeAndRun(Runnable runnable) {
        while (true) {
            SyncState sr = state.get();
            if ((sr == null || !sr.isSuccess())) {
                if (state.compareAndSet(sr, new SyncState(true))) {
                    if (runnable != null) {
                        runnable.run();
                    }
                    return true;
                }
            } else {
                return false;
            }
        }

    }

    /**
     * Handles completion of governance policy fetching by processing all pending futures.
     *
     * @param consumer the callback to apply to each pending future
     */
    private void onComplete(Consumer<CompletableFuture<Void>> consumer) {
        logger.info("Complete fetching {} {} governance policy.", fullName, type);
        synchronized (mutex) {
            futures.forEach(consumer);
            futures.clear();
        }
    }

    @Getter
    private static class SyncState {

        private final boolean success;

        private final Throwable error;

        SyncState(boolean success) {
            this(success, null);
        }

        SyncState(Throwable error) {
            this(false, error);
        }

        SyncState(boolean success, Throwable error) {
            this.success = success;
            this.error = error;
        }
    }

}
