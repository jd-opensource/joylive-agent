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
package com.jd.live.agent.core.event.config;

import lombok.Getter;
import lombok.Setter;

/**
 * publisher config
 */
@Getter
@Setter
public class PublisherConfig {

    public static final int DEFAULT_CAPACITY = 1024;

    public static final int DEFAULT_TIMEOUT = 0;

    public static final int BATCH_SIZE = 100;

    // capacity of queue
    private int capacity = DEFAULT_CAPACITY;

    // enqueue timeout
    private long timeout = DEFAULT_TIMEOUT;

    // batch size
    private int batchSize = BATCH_SIZE;

    public PublisherConfig() {
    }

    public PublisherConfig(int capacity, long timeout) {
        this.capacity = capacity;
        this.timeout = timeout;
    }

}
