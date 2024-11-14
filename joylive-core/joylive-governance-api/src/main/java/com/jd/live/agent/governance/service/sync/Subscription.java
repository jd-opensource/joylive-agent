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
package com.jd.live.agent.governance.service.sync;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a subscription to a specific data source.
 *
 * @param <K> The type of the subscription key, which must extend the {@link SyncKey} interface.
 * @param <T> The type of the data being subscribed to.
 */
public class Subscription<K extends SyncKey, T> implements SyncListener<T> {

    private static final int INTERVALS = 10;

    @Getter
    private final String owner;

    @Getter
    private final K key;

    @Setter
    private SyncListener<T> listener;

    @Getter
    @Setter
    private long version;

    private final AtomicLong counter = new AtomicLong();

    private final AtomicBoolean status = new AtomicBoolean(false);

    public Subscription(String owner, K key) {
        this(owner, key, null);
    }

    public Subscription(String owner, K key, SyncListener<T> listener) {
        this.owner = owner;
        this.key = key;
        this.listener = listener;
    }

    @Override
    public void onUpdate(SyncResponse<T> response) {
        listener.onUpdate(response);
    }

    public boolean lock() {
        return status.compareAndSet(false, true);
    }

    public boolean unlock() {
        return status.compareAndSet(true, false);
    }

    public void addCounter() {
        counter.incrementAndGet();
    }

    /**
     * Determines whether a log message should be printed based on the counter.
     *
     * @return true if a log message should be printed, false otherwise.
     */
    public boolean shouldPrint() {
        return counter.get() % INTERVALS == 1;
    }

    /**
     * Generates a success message for the synchronization.
     *
     * @param status the response code.
     * @return the success message.
     */
    public String getSuccessMessage(SyncStatus status) {
        return "Success synchronizing " + key.getType() + " policy from " + owner + ". name=" + key
                + ", status=" + status
                + ", counter=" + counter.get();
    }

    /**
     * Generates an error message for the synchronization.
     *
     * @param status  the response code.
     * @param message the message
     * @return the error message.
     */
    public String getErrorMessage(SyncStatus status, String message) {
        return "Failed to synchronize " + key.getType() + " policy from " + owner + ". name=" + key
                + ", status=" + status
                + ", message=" + message
                + ", counter=" + counter.get();
    }
}
