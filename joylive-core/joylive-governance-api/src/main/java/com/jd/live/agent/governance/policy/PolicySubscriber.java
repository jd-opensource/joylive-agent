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

@Getter
public class PolicySubscriber {

    private final String name;

    private final String namespace;

    private final PolicyType type;

    private final CompletableFuture<Void> future = new CompletableFuture<>();

    public PolicySubscriber(String name, String namespace, PolicyType type) {
        this.name = name;
        this.namespace = namespace;
        this.type = type;
    }

    public void complete() {
        future.complete(null);
    }

    public void completeExceptionally(Throwable ex) {
        future.completeExceptionally(ex);
    }

    protected void trigger(CompletableFuture<Void> other) {
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

    protected void trigger(BiConsumer<Void, Throwable> action) {
        if (action != null) {
            future.whenComplete(action);
        }
    }

}
