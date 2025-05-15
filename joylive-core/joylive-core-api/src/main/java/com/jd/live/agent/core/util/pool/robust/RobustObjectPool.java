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
package com.jd.live.agent.core.util.pool.robust;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.util.pool.ObjectPool;
import com.jd.live.agent.core.util.pool.PoolStatus;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Thread-safe, partitioned object pool implementation with:
 * <ul>
 *   <li>Automatic validation of returned objects</li>
 *   <li>Dynamic capacity management</li>
 *   <li>Lock-free concurrent operations</li>
 *   <li>Partitioned storage for reduced contention</li>
 * </ul>
 *
 * @param <T> type of pooled objects
 * @see ObjectPool
 */
@Extension("Robust")
public class RobustObjectPool<T> implements ObjectPool<T> {
    private final Queue<T>[] partitions;
    private final int partitionMask;
    private final AtomicInteger pooledCount = new AtomicInteger();
    private final AtomicInteger totalCount = new AtomicInteger();
    private final int maxCapacity;
    private final Supplier<T> objectFactory;
    private final Predicate<T> validator;

    public RobustObjectPool(Supplier<T> factory, int maxCapacity) {
        this(factory, maxCapacity, null);
    }

    public RobustObjectPool(Supplier<T> factory, int maxCapacity, Predicate<T> validator) {
        this.objectFactory = factory;
        this.maxCapacity = Math.max(1, maxCapacity);
        this.validator = validator;

        int partitionSize = 1 << (32 - Integer.numberOfLeadingZeros(Runtime.getRuntime().availableProcessors() * 2 - 1));
        this.partitions = new Queue[partitionSize];
        this.partitionMask = partitionSize - 1;

        for (int i = 0; i < partitions.length; i++) {
            partitions[i] = new ArrayDeque<>();
        }
    }

    @Override
    public T borrow() {
        int startIdx = ThreadLocalRandom.current().nextInt();
        Queue<T> q;
        T obj;
        for (int i = 0; i < partitions.length; i++) {
            q = partitions[(startIdx + i) & partitionMask];
            obj = q.poll();
            if (obj != null) {
                pooledCount.decrementAndGet();
                if (validate(obj)) {
                    return obj;
                }
                totalCount.decrementAndGet();
            }
        }

        int currentTotal;
        while (true) {
            currentTotal = totalCount.get();
            if (currentTotal >= maxCapacity) {
                // not waiting for object release
                return objectFactory.get();
            }
            if (totalCount.compareAndSet(currentTotal, currentTotal + 1)) {
                try {
                    return objectFactory.get();
                } catch (Throwable e) {
                    totalCount.decrementAndGet();
                    throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void release(T obj) {
        if (obj == null) {
            return;
        }
        if (!validate(obj)) {
            totalCount.decrementAndGet();
            return;
        }
        while (true) {
            int currentPooled = pooledCount.get();
            if (currentPooled >= maxCapacity) {
                totalCount.decrementAndGet();
                // Discard the object if the pool is full
                return;
            }
            if (pooledCount.compareAndSet(currentPooled, currentPooled + 1)) {
                int idx = ThreadLocalRandom.current().nextInt() & partitionMask;
                partitions[idx].offer(obj);
                return;
            }
        }
    }

    @Override
    public PoolStatus getStatus() {
        return new PoolStatus(pooledCount.get(), totalCount.get() - pooledCount.get(), maxCapacity);
    }

    /**
     * Internal validation check
     * @return true if object passes validation (or no validator)
     */
    private boolean validate(T obj) {
        return validator == null || validator.test(obj);
    }

}