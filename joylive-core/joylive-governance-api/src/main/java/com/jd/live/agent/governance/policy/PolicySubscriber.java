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

import com.jd.live.agent.governance.policy.service.ServiceName;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Represents a subscriber to a policy, encapsulating the subscription details and providing mechanisms to track
 * the completion status of the subscription. It allows for asynchronous notification upon completion or failure
 * of the policy subscription process.
 */
@Getter
public class PolicySubscriber implements ServiceName {

    private final String name;

    private final String namespace;

    private final String type;

    private final Map<String, AtomicBoolean> states;

    private final AtomicInteger counter;

    private final CompletableFuture<Void> future = new CompletableFuture<>();

    /**
     * Constructs a new instance of a policy subscriber with the specified name, namespace, and policy type.
     *
     * @param name      The name of the subscriber.
     * @param namespace The namespace of the subscriber.
     * @param type      The type of the subscriber.
     * @param owners    The owner of the subscriber.
     */
    public PolicySubscriber(String name, String namespace, String type, List<String> owners) {
        this.name = name;
        this.namespace = namespace;
        this.type = type;
        this.states = owners == null || owners.isEmpty() ? null
                : owners.stream().collect(Collectors.toMap(o -> o, o -> new AtomicBoolean(false)));
        this.counter = states == null ? null : new AtomicInteger(states.size());
    }

    /**
     * Marks the subscription process as complete successfully. This method completes the associated future
     * normally, indicating that the subscription process has finished without errors.
     *
     * @param owner The owner whose subscription process is marked as complete.
     * @return {@code true} if the completion was successful, {@code false} otherwise.
     */
    public boolean complete(String owner) {
        if (states == null) {
            return future.complete(null);
        }
        AtomicBoolean done = owner == null ? null : states.get(owner);
        if (done != null && done.compareAndSet(false, true)) {
            if (counter.decrementAndGet() == 0) {
                future.complete(null);
            }
            return true;
        }
        return false;
    }

    /**
     * Completes the asynchronous operation represented by this class.
     *
     * @return true if the operation was successfully completed, false otherwise.
     */
    public boolean complete() {
        if (states == null) {
            return future.complete(null);
        }
        for (Map.Entry<String, AtomicBoolean> entry : states.entrySet()) {
            if (entry.getValue().compareAndSet(false, true)) {
                if (counter.decrementAndGet() == 0) {
                    return future.complete(null);
                }
            }
        }
        return false;
    }

    /**
     * Checks if the associated future has completed successfully.
     *
     * @return {@code true} if the future is done and has not completed exceptionally,
     * otherwise {@code false}
     */
    public boolean isDone() {
        return future.isDone() && !future.isCompletedExceptionally();
    }

    /**
     * Marks the subscription process as complete with an exception. This method completes the associated future
     * exceptionally, indicating that the subscription process has finished due to an error.
     *
     * @param ex The exception to complete the future with, representing the error that occurred during the
     *           subscription process.
     * @return {@code true} if the future was completed exceptionally, {@code false} otherwise.
     */
    public boolean completeExceptionally(Throwable ex) {
        boolean result = future.completeExceptionally(ex);
        if (result && states != null) {
            states.forEach((key, value) -> value.compareAndSet(false, true));
        }
        return result;
    }

    /**
     * Triggers another CompletableFuture based on the completion status of this subscriber's future.
     * If this subscriber's future completes normally, the other future is also completed normally. If this
     * subscriber's future completes exceptionally, the other future is completed exceptionally with the same
     * exception.
     *
     * @param other The CompletableFuture to be triggered based on the completion status of this subscriber's future.
     */
    public void trigger(CompletableFuture<Void> other) {
        if (other != null) {
            future.whenComplete((v, e) -> {
                if (e == null) {
                    other.complete(v);
                } else {
                    other.completeExceptionally(e);
                }
            });
        }
    }

    /**
     * Registers an action to be triggered upon completion of the subscription process. The action is provided
     * with the result (if the process completes normally) or the exception (if the process completes exceptionally).
     *
     * @param action The action to be performed upon completion of the subscription process, accepting either
     *               the result of the completion or the exception thrown.
     */
    public void trigger(BiConsumer<Void, Throwable> action) {
        if (action != null) {
            future.whenComplete(action);
        }
    }

}
