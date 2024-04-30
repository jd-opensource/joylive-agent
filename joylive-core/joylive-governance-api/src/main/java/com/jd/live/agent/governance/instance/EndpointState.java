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
package com.jd.live.agent.governance.instance;

/**
 * Enumerates the possible states of an endpoint in a distributed system.
 * These states can be used to represent the current health or operational status of the endpoint.
 */
public enum EndpointState {

    /**
     * The endpoint is in the warmup phase, typically after being started, and not yet ready to handle full traffic.
     */
    WARMUP {
        @Override
        public boolean isAccessible() {
            return true;
        }
    },

    /**
     * The endpoint is healthy and fully operational, capable of handling requests.
     */
    HEALTHY {
        @Override
        public boolean isAccessible() {
            return true;
        }
    },

    /**
     * The endpoint is experiencing some issues but can still handle requests, albeit possibly at a reduced capacity or with increased latency.
     */
    WEAK {
        @Override
        public boolean isAccessible() {
            return true;
        }
    },

    /**
     * The endpoint has been temporarily suspended, possibly for maintenance or due to issues, and cannot handle requests.
     */
    SUSPEND,

    /**
     * The endpoint is in the process of recovering from a suspended or weakened state and may soon become healthy.
     */
    RECOVER {
        @Override
        public boolean isAccessible() {
            return true;
        }
    },

    /**
     * The endpoint has been disabled and is not participating in handling requests, possibly due to configuration or significant issues.
     */
    DISABLE,

    /**
     * The endpoint is in the process of shutting down or being removed from service and cannot handle new requests.
     */
    CLOSING;

    public boolean isAccessible() {
        return false;
    }

}
