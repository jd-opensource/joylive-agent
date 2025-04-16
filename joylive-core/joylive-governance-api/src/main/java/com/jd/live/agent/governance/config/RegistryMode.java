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
package com.jd.live.agent.governance.config;

/**
 * Represents the mode of operation for a registry.
 * This enum defines the possible modes that can be used when interacting with a registry,
 * such as registering, subscribing, or performing both operations.
 */
public enum RegistryMode {

    /**
     * Represents the mode where only registration operations are performed.
     * In this mode, entities are added to the registry but no subscription logic is executed.
     */
    REGISTER {
        @Override
        public boolean isSubscribe() {
            return false;
        }
    },

    /**
     * Represents the mode where only subscription operations are performed.
     * In this mode, entities are subscribed to events or updates but no registration logic is executed.
     */
    SUBSCRIBE {
        @Override
        public boolean isRegister() {
            return false;
        }
    },

    /**
     * Represents the mode where both registration and subscription operations are performed.
     * In this mode, entities are added to the registry and also subscribed to events or updates.
     */
    FULL,

    NONE {
        @Override
        public boolean isRegister() {
            return false;
        }

        @Override
        public boolean isSubscribe() {
            return false;
        }
    };


    public boolean isRegister() {
        return true;
    }

    public boolean isSubscribe() {
        return true;
    }

}
