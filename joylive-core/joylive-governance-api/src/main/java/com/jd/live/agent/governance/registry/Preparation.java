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
package com.jd.live.agent.governance.registry;

/**
 * A checker interface for handling subscription success and failure outcomes.
 *
 * @param <T> the return type for success and failure handlers
 */
public interface Preparation<T> {

    /**
     * Called when subscription succeeds.
     *
     * @return the success result
     */
    T onSuccess();

    /**
     * Called when subscription fails.
     *
     * @return the failure result
     */
    T onFailure();

    /**
     * Boolean implementation of ServicePreparation that returns true for success and false for failure.
     */
    class BooleanPreparation implements Preparation<Boolean> {

        /**
         * Singleton instance.
         */
        public static final BooleanPreparation INSTANCE = new BooleanPreparation();

        @Override
        public Boolean onSuccess() {
            return true;
        }

        @Override
        public Boolean onFailure() {
            return false;
        }
    }
}
