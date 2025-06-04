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
package com.jd.live.agent.governance.probe;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages failover between multiple server addresses in distributed systems.
 * Handles connection failures and provides recovery mechanisms.
 */
public interface FailoverAddressList {

    /**
     * @return Current active server address
     */
    String current();

    /**
     * @return First available server address in the list
     */
    String first();

    /**
     * Switches to next available server address
     */
    void next();

    /**
     * Resets to initial server address
     */
    void reset();

    /**
     * @return Total number of available server addresses
     */
    int size();

    class SimpleAddressList implements FailoverAddressList {

        private final List<String> addresses;

        private final AtomicLong index = new AtomicLong(0);

        private final int size;

        public SimpleAddressList(List<String> addresses) {
            this.addresses = addresses;
            this.size = addresses == null ? 0 : addresses.size();
        }

        @Override
        public String current() {
            return size == 0 ? null : addresses.get((int) (index.get() % size));
        }

        @Override
        public String first() {
            return size == 0 ? null : addresses.get(0);
        }

        @Override
        public void next() {
            index.incrementAndGet();
        }

        @Override
        public void reset() {
            index.set(0);
        }

        @Override
        public int size() {
            return size;
        }
    }
}
