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
package com.jd.live.agent.plugin.transmission.thread.adapter;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.thread.Snapshot;

import java.util.concurrent.Callable;

/**
 * AbstractThreadAdapter
 */
public abstract class AbstractThreadAdapter<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractThreadAdapter.class);

    protected final String name;

    private final Runnable runnable;

    private final Callable<T> callable;

    private final Snapshot[] snapshots;

    public AbstractThreadAdapter(String name, Runnable runnable, Callable<T> callable, Snapshot[] snapshots) {
        this.name = name;
        this.runnable = runnable;
        this.callable = callable;
        this.snapshots = snapshots;
    }

    public void run() {
        try {
            before();
            runnable.run();
        } finally {
            after();
        }
    }

    public T call() throws Exception {
        try {
            before();
            return callable.call();
        } finally {
            after();
        }
    }

    protected void after() {
        for (Snapshot snapshot : snapshots) {
            try {
                snapshot.remove();
            } catch (Exception e) {
                logger.error("failed to remove snapshot at thread {}, caused by {}", Thread.currentThread().getName(), e.getMessage());
            }
        }
    }

    protected void before() {
        for (Snapshot snapshot : snapshots) {
            try {
                snapshot.restore();
            } catch (Throwable e) {
                logger.error("failed to restore snapshot at thread {}, caused by {}", Thread.currentThread().getName(), e.getMessage());
            }
        }
    }
}