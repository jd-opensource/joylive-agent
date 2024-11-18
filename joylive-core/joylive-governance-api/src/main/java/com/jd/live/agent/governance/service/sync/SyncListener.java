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

/**
 * A functional interface for listening to synchronization events.
 *
 * @param <T> Type of the synchronized data.
 */
@FunctionalInterface
public interface SyncListener<T> {

    /**
     * Called when a synchronization update is received.
     *
     * @param response The synchronization response containing the updated data.
     */
    void onUpdate(SyncResponse<T> response);
}

