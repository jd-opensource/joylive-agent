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
package com.jd.live.agent.governance.counter.internal;

import com.jd.live.agent.governance.counter.FlyingCounter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks in-flight requests count for graceful shutdown.
 * Provides await mechanism with timeout support.
 */
public class InternalFlyingCounter implements FlyingCounter {

    private final AtomicInteger counter = new AtomicInteger(0);

    private final AtomicBoolean done = new AtomicBoolean(false);

    private final CompletableFuture<Boolean> future = new CompletableFuture<>();

    @Override
    public void done() {
        if (done.compareAndSet(false, true)) {
            future.complete(true);
        }
    }

    @Override
    public void increment() {
        counter.incrementAndGet();
    }

    @Override
    public int decrement() {
        return counter.decrementAndGet();
    }

    @Override
    public int getCount() {
        return counter.get();
    }

    @Override
    public CompletableFuture<Boolean> waitDone() {
        if (counter.get() == 0) {
            done();
        }
        return future;
    }
}
