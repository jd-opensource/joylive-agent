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

package com.jd.live.agent.bootstrap.bytekit.context;

/**
 * An interface for managing locks in a thread-safe manner.
 */
public interface LockContext {

    /**
     * Attempts to acquire a lock with the specified ID.
     *
     * @param id the ID of the lock to acquire
     * @return true if the lock was successfully acquired, false otherwise
     */
    boolean tryLock(long id);

    /**
     * Releases the lock with the specified ID.
     *
     * @param id the ID of the lock to release
     * @return true if the lock was successfully released, false otherwise
     */
    boolean unlock(long id);

    /**
     * Checks if a lock with the specified ID is currently held.
     *
     * @param id the ID of the lock to check
     * @return true if the lock is currently held, false otherwise
     */
    boolean isLocked(long id);

    /**
     * A default implementation of the LockContext interface.
     */
    class DefaultLockContext implements LockContext {

        /**
         * A thread-local variable used to store the ID of the currently held lock.
         */
        private final ThreadLocal<Long> local = new ThreadLocal<>();

        @Override
        public boolean tryLock(long id) {
            if (local.get() == null) {
                local.set(id);
                return true;
            }
            return false;
        }

        @Override
        public boolean unlock(long id) {
            if (isLocked(id)) {
                local.remove();
                return true;
            }
            return false;
        }

        @Override
        public boolean isLocked(long id) {
            Long value = local.get();
            return value != null && value == id;
        }
    }

}
