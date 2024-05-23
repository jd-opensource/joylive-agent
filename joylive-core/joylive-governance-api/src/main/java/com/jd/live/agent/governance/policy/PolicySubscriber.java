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

import lombok.Getter;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Represents a subscriber to a policy, encapsulating the subscription details and providing mechanisms to track
 * the completion status of the subscription. It allows for asynchronous notification upon completion or failure
 * of the policy subscription process.
 */
@Getter
public class PolicySubscriber {

    private final String name;

    private final String namespace;

    private final PolicyType type;

    private final CompletableFuture<Void> future = new CompletableFuture<>();

    /**
     * Constructs a new instance of a policy subscriber with the specified name, namespace, and policy type.
     *
     * @param name      The name of the subscriber.
     * @param namespace The namespace of the subscriber.
     * @param type      The type of policy the subscriber is interested in.
     */
    public PolicySubscriber(String name, String namespace, PolicyType type) {
        this.name = name;
        this.namespace = namespace;
        this.type = type;
    }

    /**
     * Marks the subscription process as complete successfully. This method completes the associated future
     * normally, indicating that the subscription process has finished without errors.
     */
    public boolean complete() {
        if (!isDone()) {
            return future.complete(null);
        }
        return false;
    }

    /**
     * Checks if the subscription process is complete.
     *
     * @return {@code true} if the subscription process has completed (either normally or exceptionally),
     *         {@code false} otherwise.
     */
    public boolean isDone() {
        return future.isDone();
    }

    /**
     * Marks the subscription process as complete with an exception. This method completes the associated future
     * exceptionally, indicating that the subscription process has finished due to an error.
     *
     * @param ex The exception to complete the future with, representing the error that occurred during the
     *           subscription process.
     */
    public boolean completeExceptionally(Throwable ex) {
        return future.completeExceptionally(ex);
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
